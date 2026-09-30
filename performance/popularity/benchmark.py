"""Local synthetic SQL benchmark; requires a dedicated empty MySQL container.

python3 performance/popularity/benchmark.py --container NAME --seed
python3 performance/popularity/benchmark.py --container NAME --phase before
python3 performance/popularity/benchmark.py --container NAME --phase after
"""
import argparse
import json
from pathlib import Path
import re
import statistics
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--container", required=True)
parser.add_argument("--rows", type=int, default=2_610_000)
parser.add_argument("--seed", action="store_true")
parser.add_argument("--phase", choices=("before", "after"))
parser.add_argument("--refresh", action="store_true", help="Measure one full refresh with each index, after query benchmarks")
parser.add_argument("--baseline", type=Path, help="Assert the same IDs and order as a before result")
parser.add_argument("--output", type=Path, default=Path("/tmp/popularity-result.json"))
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]


def sql(statement):
    result = subprocess.run(
        ["docker", "exec", "-i", args.container, "mysql", "--default-character-set=utf8mb4", "-uroot", "-NB", "popularity_bench"],
        input=statement, text=True, capture_output=True,
    )
    if result.returncode:
        raise RuntimeError(result.stderr)
    return result.stdout.strip()


if args.seed:
    # Refuse to overwrite any existing schema. This benchmark owns only this database.
    assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='popularity_bench'") == "0"
    migrations = sorted((root / "src/main/resources/db/migration").glob("V*.sql"),
                        key=lambda path: int(path.name.split("__")[0][1:]))
    for migration in migrations:
        if int(migration.name.split("__")[0][1:]) <= 21:
            sql(migration.read_text())
    sql("""
        CREATE TABLE bench_digit (n INT PRIMARY KEY);
        INSERT INTO bench_digit VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);
        CREATE TABLE bench_sequence (n INT PRIMARY KEY);
        INSERT INTO bench_sequence
        SELECT a.n+10*b.n+100*c.n+1000*d.n+10000*e.n+1
        FROM bench_digit a CROSS JOIN bench_digit b CROSS JOIN bench_digit c
        CROSS JOIN bench_digit d CROSS JOIN bench_digit e;
        INSERT INTO book_category (id,name,path) VALUES (9001,'broad','broad'),(9002,'sparse','sparse');
        """)
    for offset in range(0, args.rows, 100_000):
        end = min(args.rows, offset + 100_000)
        sql(f"""
            INSERT INTO books (id,title,author,publisher,category,published_at)
            SELECT n+{offset}, CONCAT('synthetic book ',n+{offset}), 'author','publisher','fiction',
                   IF(MOD(n+{offset},2)=0,'2026-01-01','2020-01-01')
            FROM bench_sequence WHERE n+{offset}<={end};
            INSERT INTO products (id,book_id,name,sale_price,discounted_price,cost_price,stock_quantity)
            SELECT n+{offset},n+{offset},CONCAT('synthetic product ',n+{offset}),20000,18000,12000,10
            FROM bench_sequence WHERE n+{offset}<={end};
            INSERT INTO product_popularity_snapshots (product_id,sales_quantity,review_count,review_rate,refreshed_at)
            SELECT n+{offset},IF(MOD(n+{offset},1000)=0,MOD(n+{offset},17)+1,0),0,0,NOW(6)
            FROM bench_sequence WHERE n+{offset}<={end};
            INSERT INTO product_category (category_id,product_id)
            SELECT 9001,n+{offset} FROM bench_sequence WHERE n+{offset}<={end} AND MOD(n+{offset},10)=0;
            INSERT INTO product_category (category_id,product_id)
            SELECT 9002,n+{offset} FROM bench_sequence WHERE n+{offset}<={end} AND MOD(n+{offset},100000)=0;
            """)
        print(f"seeded {end}", flush=True)
    sql("ANALYZE TABLE products,books,product_popularity_snapshots,product_category,book_category")

if args.phase:
    if args.phase == "after":
        columns = sql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() "
                      "AND table_name='product_popularity_snapshots' AND index_name='idx_product_popularity_snapshots_order'")
        if columns == "4":
            sql((root / "src/main/resources/db/migration/V22__align_product_popularity_order_index.sql").read_text())
        else:
            assert columns == "2"
        sql("ANALYZE TABLE product_popularity_snapshots")
    sales = "COALESCE(s.sales_quantity,0)" if args.phase == "before" else "s.sales_quantity"
    key = "p.id" if args.phase == "before" else "s.product_id"
    source = ("products p LEFT JOIN product_popularity_snapshots s ON s.product_id=p.id"
              if args.phase == "before" else "product_popularity_snapshots s JOIN products p ON p.id=s.product_id")

    def query(extra=""):
        if args.phase == "after":
            return f"""SELECT s.product_id,s.sales_quantity,s.review_count,s.review_rate
            FROM product_popularity_snapshots s
            WHERE s.product_id = (SELECT p.id FROM products p JOIN books b ON b.id=p.book_id
                WHERE p.id=s.product_id AND p.deleted_at IS NULL AND b.deleted_at IS NULL
                {"AND b.published_at BETWEEN '2026-01-01' AND '2026-12-31'" if "published_at" in extra else ""})
            {"" if "published_at" in extra else extra.replace("pc.product_id=p.id", "pc.product_id=s.product_id")}
            ORDER BY s.sales_quantity DESC,s.product_id DESC LIMIT 21"""
        reviews = "COALESCE(s.review_count,0),COALESCE(s.review_rate,0)" if args.phase == "before" else "s.review_count,s.review_rate"
        return f"""SELECT p.*,b.*,{sales},{reviews}
        FROM {source} JOIN books b ON b.id=p.book_id
        WHERE p.deleted_at IS NULL AND b.deleted_at IS NULL {extra}
        ORDER BY {sales} DESC,{key} DESC LIMIT 21"""

    def execute(statement):
        ranked = sql(statement)
        if args.phase == "after" and ranked:
            ids = [int(row.split("\t")[0]) for row in ranked.splitlines()]
            hydrated = sql("SELECT p.*,b.* FROM products p JOIN books b ON b.id=p.book_id WHERE p.id IN ("
                           + ",".join(map(str, ids)) + ")")
            assert sorted(int(row.split("\t")[0]) for row in hydrated.splitlines()) == sorted(ids)
        return ranked

    first = execute(query()).splitlines()
    last = first[19].split("\t")
    # p.id is the first field; the three snapshot fields are last.
    cursor = f"AND ({sales} < {last[-3]} OR ({sales} = {last[-3]} AND {key} < {last[0]}))"
    category = """AND EXISTS (SELECT 1 FROM product_category pc JOIN book_category c ON c.id=pc.category_id
        WHERE pc.product_id=p.id AND pc.deleted_at IS NULL AND c.deleted_at IS NULL AND c.id={})"""
    cases = {"first": "", "second": cursor, "broad_category": category.format(9001),
             "sparse_category": category.format(9002), "published_range": "AND b.published_at BETWEEN '2026-01-01' AND '2026-12-31'"}
    result = {"phase": args.phase, "mysql": sql("SELECT VERSION()"),
              "counts": sql("SELECT (SELECT COUNT(*) FROM products),(SELECT COUNT(*) FROM product_popularity_snapshots)"),
              "method": "1 warm-up + 3 timed mysql CLI runs per case; after includes ranking and hydration; includes CLI/IPC; synthetic rows; LIMIT 21",
              "optimizer_switch": sql("SELECT @@optimizer_switch"), "buffer_pool": sql("SELECT @@innodb_buffer_pool_size"), "cases": {}}
    for name, extra in cases.items():
        statement = query(extra)
        expected = execute(statement)
        samples = []
        for _ in range(3):
            start = time.perf_counter()
            actual = execute(statement)
            samples.append((time.perf_counter() - start) * 1000)
            assert actual == expected
        result["cases"][name] = {"milliseconds": samples, "median_ms": statistics.median(samples),
                                  "ids": [int(row.split("\t")[0]) for row in expected.splitlines()],
                                  "sql": statement, "explain": sql("EXPLAIN " + statement),
                                  "analyze": sql("EXPLAIN ANALYZE " + statement)}
        if args.baseline:
            baseline = json.loads(args.baseline.read_text())
            assert result["cases"][name]["ids"] == baseline["cases"][name]["ids"], name
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
        print(f"{name}: {statistics.median(samples):.2f} ms", flush=True)

if args.refresh:
    source = (root / "src/main/java/com/book/core/product/infrastructure/persistence/repository/ProductQueryRepository.java").read_text()
    insert = re.search(r'INSERT_POPULARITY_SNAPSHOTS_SQL = """(.*?)""";', source, re.S).group(1)
    result = {"method": "one committed DELETE + actual repository INSERT SELECT per index; orders/reviews empty; CLI/IPC included",
              "cases": {}}
    for phase, columns in (("before", "sales_quantity DESC,review_count DESC,review_rate DESC,product_id DESC"),
                           ("after", "sales_quantity DESC,product_id DESC")):
        sql(f"ALTER TABLE product_popularity_snapshots DROP INDEX idx_product_popularity_snapshots_order, "
            f"ADD INDEX idx_product_popularity_snapshots_order ({columns})")
        start = time.perf_counter()
        sql("START TRANSACTION; DELETE FROM product_popularity_snapshots; " + insert + "; COMMIT;")
        elapsed = (time.perf_counter() - start) * 1000
        result["cases"][phase] = {"milliseconds": elapsed, "rows": sql("SELECT COUNT(*) FROM product_popularity_snapshots")}
        # Restore the synthetic nonzero ranks; refresh source intentionally has no orders.
        sql("UPDATE product_popularity_snapshots SET sales_quantity=MOD(product_id,17)+1 WHERE MOD(product_id,1000)=0")
        print(f"refresh {phase}: {elapsed:.2f} ms", flush=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n")
