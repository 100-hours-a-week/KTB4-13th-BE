const assert = require('node:assert/strict');
const { existsSync, readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

const root = join(__dirname, '../..');
const read = path => readFileSync(join(root, path), 'utf8');

test('PR 크기 라벨과 대기 알림 workflow 계약을 유지한다', () => {
  const labels = read('.github/labels.yml');
  const sizeLabeler = read('.github/workflows/pr-labeler.yml');
  const pendingAlarm = read('.github/workflows/pending-pr-alarm.yml');

  for (const label of [
    'size/S',
    'size/M',
    'size/L',
    'size/XL',
    'size/XXL',
    'size/XXL-exempt',
    'review-needed',
    'merge-pending',
  ]) {
    assert.match(labels, new RegExp(`name: "${label.replace('/', '\\/')}"`));
  }
  for (const [label, color] of Object.entries({
    feat: '1d76db',
    fix: 'd73a4a',
    refactor: '6f42c1',
    test: '0e8a16',
    bug: 'b60205',
    chore: '6a737d',
    'size/S': '0e8a16',
    'size/M': 'fbca04',
    'size/L': 'f9a825',
    'size/XL': 'b60205',
    'size/XXL': '8b0000',
    'size/XXL-exempt': '6a737d',
    'review-needed': 'f9d0c4',
    'merge-pending': 'd4c5f9',
  })) {
    assert.match(labels, new RegExp(`name: "${label.replace('/', '\\/')}"\\n  color: "${color}"`));
  }
  assert.match(sizeLabeler, /types: \[opened, reopened, synchronize, ready_for_review, labeled, unlabeled\]/);
  assert.match(sizeLabeler, /pull-requests: write/);
  assert.match(sizeLabeler, /'refactor\/': 'refactor'/);
  assert.match(sizeLabeler, /changes <= 50/);
  assert.match(sizeLabeler, /changes <= 200/);
  assert.match(sizeLabeler, /changes <= 500/);
  assert.match(sizeLabeler, /changes <= 1500/);
  assert.match(sizeLabeler, /size\/XXL-exempt/);
  assert.match(sizeLabeler, /REQUEST_CHANGES/);
  assert.match(sizeLabeler, /DISMISS/);
  assert.match(sizeLabeler, /pulls\.dismissReview/);
  assert.match(sizeLabeler, /pr-size-xxl-guard/);
  assert.match(pendingAlarm, /cron: '0 \* \* \* \*'/);
  assert.match(pendingAlarm, /now - createdAt >= oneDay/);
  assert.match(pendingAlarm, /now - approvalAt >= oneDay/);
  assert.equal(existsSync(join(root, '.github/workflows/sync-label.yml')), false);
});

test('Backend CI가 SHA 이미지를 게시하고 배포 책임은 Cloud Repository에 둔다', () => {
  const ciWorkflow = read('.github/workflows/ci.yml');
  const buildCompose = read('compose.build.yml');

  assert.match(ciWorkflow, /AWS_CI_ROLE_ARN/);
  assert.match(ciWorkflow, /docker compose -f compose\.build\.yml build backend/);
  assert.match(ciWorkflow, /docker compose -f compose\.build\.yml push backend/);
  assert.equal(existsSync(join(root, '.github/workflows/cd.yml')), false);

  assert.match(buildCompose, /image: \$\{BACKEND_IMAGE:\?BACKEND_IMAGE is required\}/);
  assert.match(buildCompose, /dockerfile: Dockerfile/);
});
