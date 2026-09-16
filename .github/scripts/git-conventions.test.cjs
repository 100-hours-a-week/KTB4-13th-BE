const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

// 워크플로의 실제 script 블록을 실행하여 검증 규칙을 복제하지 않는다.
const workflow = readFileSync(join(__dirname, '../workflows/git-conventions.yml'), 'utf8');
const block = workflow.match(/^          script: \|\n((?: {12}[^\n]*\n|\n)+)/m);
assert.ok(block, '워크플로의 script 블록을 찾을 수 없습니다.');
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const check = new AsyncFunction('github', 'context', 'core', block[1].replace(/^ {12}/gm, ''));

async function run({ title = 'feat: 샘플 생성 API 추가', messages = ['feat: 샘플 생성 API 추가(#123)'],
  mutatePage, apiError } = {}) {
  const failures = [];
  const logs = [];
  let requests = 0;
  const context = {
    repo: { owner: 'test-owner', repo: 'test-repo' },
    payload: { pull_request: { number: 7, title, head: { sha: 'head-sha' } } },
  };
  const github = {
    async graphql(query, variables) {
      if (apiError) throw apiError;
      assert.match(query, /commits\(first: 100, after: \$cursor\)/);
      assert.deepEqual(variables, { ...context.repo, number: 7, cursor: requests === 0 ? null : `page-${requests}` });
      const start = requests * 100;
      requests += 1;
      const current = {
        headRefOid: 'head-sha',
        commits: {
          totalCount: messages.length,
          nodes: messages.slice(start, start + 100).map((message, index) => ({
            commit: { oid: String(start + index + 1).padStart(40, 'a'), message },
          })),
          pageInfo: { hasNextPage: start + 100 < messages.length, endCursor: `page-${requests}` },
        },
      };
      if (mutatePage) mutatePage(current);
      return { repository: { pullRequest: current } };
    },
  };
  await check(github, context, { setFailed: value => failures.push(value), info: value => logs.push(value) });
  return { failures, logs, requests };
}

test('허용된 6개 타입, 한국어 요약, 선택적 커밋 본문을 허용한다', async () => {
  for (const type of ['feat', 'fix', 'refactor', 'test', 'docs', 'chore']) {
    const result = await run({ title: `${type}: 동작 개선`, messages: [`${type}: 동작 개선(#123)\n\n변경 이유`] });
    assert.deepEqual(result.failures, []);
  }
});

test('누락된 이슈 번호, 잘못된 타입, 빈 요약, 마침표와 무의미한 메시지를 거부한다', async () => {
  for (const message of ['feat: 구현', 'build: 구현(#1)', 'feat: (#1)', 'feat:   (#1)',
    'feat: 구현.(#1)', 'feat: 구현 (#1)', 'feat: 구현(#0)', 'feat: 구현(#01)',
    'feat: WIP(#1)', 'fix: update(#1)', 'chore: change(#1)', 'Merge branch main']) {
    assert.equal((await run({ messages: [message] })).failures.length, 1, message);
  }
});

test('본문에만 올바른 형식이 있어도 커밋 첫 줄이 잘못되면 실패한다', async () => {
  const result = await run({ messages: ['잘못된 제목\n\nfeat: 정상 형식(#123)'] });
  assert.equal(result.failures.length, 1);
});

test('PR 제목에 이슈 번호, 여러 줄, 빈 요약과 잘못된 타입을 허용하지 않는다', async () => {
  for (const title of ['feat: 구현(#123)', 'feat: 구현\n', 'feat: 구현\n다른 줄', 'FEAT: 구현',
    'feat: ', 'feat:  구현', 'feat: 구현.', 'feat: WIP']) {
    assert.match((await run({ title })).failures[0], /PR 제목/);
  }
});

test('100개와 250개를 넘는 PR의 마지막 커밋까지 검사한다', async () => {
  const messages = Array(301).fill('test: 정상 동작 검증(#123)');
  let result = await run({ messages });
  assert.equal(result.requests, 4);
  assert.deepEqual(result.failures, []);
  assert.match(result.logs[0], /301개/);
  messages[300] = '잘못된 마지막 커밋';
  result = await run({ messages });
  assert.equal(result.requests, 4);
  assert.equal(result.failures.length, 1);
});

test('조회 실패, 누락된 커밋과 검사 중 변경된 head는 성공으로 처리하지 않는다', async () => {
  await assert.rejects(run({ apiError: new Error('API unavailable') }), /API unavailable/);
  await assert.rejects(run({ messages: [] }), /불완전/);
  await assert.rejects(run({ mutatePage: current => { current.commits.totalCount += 1; } }), /불완전/);
  await assert.rejects(run({ mutatePage: current => { current.headRefOid = 'changed'; } }), /변경/);
  await assert.rejects(run({ mutatePage: current => {
    current.commits.pageInfo = { hasNextPage: true, endCursor: null };
  } }), /끝까지/);
});

test('PR 제목과 메시지의 코드처럼 보이는 문자열을 데이터로만 처리한다', async () => {
  const summary = '인용 "값", `코드`, ${process.exit(99)} 처리';
  const result = await run({ title: `fix: ${summary}`, messages: [`fix: ${summary}(#123)`] });
  assert.deepEqual(result.failures, []);
});
