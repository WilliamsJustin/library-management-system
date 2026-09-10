# -*- coding: utf-8 -*-
"""End-to-end verification of reader self-borrow (POST /api/loans/self)."""
import json
import urllib.request
import urllib.error

BASE = 'http://localhost:8080/api'


def call(method, path, body=None, token=None):
    url = BASE + path
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            txt = r.read().decode()
            return r.status, (json.loads(txt) if txt else None)
    except urllib.error.HTTPError as e:
        txt = e.read().decode()
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, txt


def login(account):
    st, body = call('POST', '/auth/login', {'account': account, 'password': 'pass123'})
    assert st == 200, f'login {account} failed: {st} {body}'
    return body['token'], body


def borrow_flow(account):
    token, me = login(account)
    print(f'== {account} login OK (role={me.get("role")}, readerType={me.get("readerType")})')

    st, page = call('GET', '/books?size=50', token=token)
    assert st == 200, f'list books failed: {st} {page}'
    book = next(b for b in page['content'] if b['availableCopies'] > 0)
    print(f'   target book: {book["title"]} (id={book["id"]}, available={book["availableCopies"]})')

    st, copies = call('GET', f'/books/{book["id"]}/copies', token=token)
    assert st == 200, f'list copies failed: {st} {copies}'
    copy = next(c for c in copies if c['status'] == 'IN_STOCK')
    print(f'   target copy: barcode={copy["barcode"]} (id={copy["id"]})')

    st, loan = call('POST', '/loans/self', {'copyId': copy['id']}, token=token)
    print(f'   POST /loans/self -> {st}: {json.dumps(loan, ensure_ascii=False)[:220]}')
    assert st == 200, f'borrow failed for {account}'
    loan_id = loan['id']

    st, copies2 = call('GET', f'/books/{book["id"]}/copies', token=token)
    c2 = next(c for c in copies2 if c['id'] == copy['id'])
    print(f'   copy status after borrow: {c2["status"]}')
    assert c2['status'] == 'BORROWED', 'copy did not flip to BORROWED'

    st, my = call('GET', '/loans/my?size=50', token=token)
    ids = [l['id'] for l in my['content']]
    print(f'   /loans/my contains new loan {loan_id}: {loan_id in ids} (total loans: {len(ids)})')
    assert loan_id in ids, 'new loan missing from /loans/my'
    return loan_id


# 1) student self-borrow
s_loan = borrow_flow('student1')
# 2) teacher self-borrow
t_loan = borrow_flow('teacher1')

# 3) admin returns both to leave clean state
admin_token, _ = login('admin1')
for lid in (s_loan, t_loan):
    st, body = call('POST', f'/loans/{lid}/return', token=admin_token)
    print(f'admin return loan {lid} -> {st}')
    assert st == 200, f'admin return failed: {st} {body}'

print()
print('ALL CHECKS PASSED: student & teacher can self-borrow; admin return works.')
