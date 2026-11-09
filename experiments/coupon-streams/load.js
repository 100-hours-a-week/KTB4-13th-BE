import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import exec from 'k6/execution';
import http from 'k6/http';
import { Counter } from 'k6/metrics';

const success = new Counter('issued_or_accepted');
const unexpected = new Counter('unexpected');
const expected = Number(__ENV.EXPECTED_STATUS);
const key = encoding.b64decode(__ENV.JWT_SECRET, 'std');
const encode = value => encoding.b64encode(JSON.stringify(value), 'rawurl');
const header = encode({ alg: 'HS512', typ: 'JWT' });

export const options = {
    scenarios: {
        issue: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE),
            duration: __ENV.DURATION + 's',
            timeUnit: '1s',
            preAllocatedVUs: 300,
            maxVUs: 1000,
        },
    },
    summaryTrendStats: ['avg', 'med', 'max', 'p(95)', 'p(99)'],
    thresholds: { unexpected: ['count==0'] },
};

export default function () {
    const iteration = exec.scenario.iterationInTest;
    const userId = Number(__ENV.USER_BASE) + iteration;
    const couponId = Number(__ENV.COUPON_ID) + iteration % Number(__ENV.COUPON_COUNT || '1');
    const iat = Math.floor(Date.now() / 1000);
    const payload = encode({ sub: String(userId), iat, exp: iat + 3600, tokenType: 'ACCESS', jti: String(userId) });
    const unsigned = header + '.' + payload;
    const jwt = unsigned + '.' + crypto.hmac('sha512', key, unsigned, 'base64rawurl');
    const response = http.post(__ENV.BASE_URL + '/api/v1/coupons/' + couponId + '/issue', null, {
        headers: { Authorization: 'Bearer ' + jwt },
    });
    success.add(Number(response.status === expected));
    unexpected.add(Number(response.status !== expected));
}

export function handleSummary(data) {
    return { [__ENV.SUMMARY_FILE]: JSON.stringify(data.metrics, null, 2) };
}
