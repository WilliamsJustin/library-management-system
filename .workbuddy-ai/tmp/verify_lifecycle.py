# -*- coding: utf-8 -*-
"""Full reader self-service lifecycle E2E: borrow -> renew -> renew-again(reject) -> return."""
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
    return body['token']


def main():
    token = login('student1')
    print('== student1 login OK (reader)')

    # borrow a copy of book 1
    st, copies = call('GET', '/books/1/copies', token=token)
    copy = next(c for c in copies if c['status'] == 'IN_STOCK')
    st, loan = call('POST', '/loans/self', {'copyId': copy['id']}, token=token)
    print(f'   borrow copy {copy["id"]} -> {st}, loanId={loan["id"]}, due={loan["dueDate"]}, renewed={loan["renwedCount"] if "renwedCount" in loan else loan.get("renewedCount")}')
    assert st == 200, f'borrow failed: {loan}'
    loan_id = loan['id']
    due0 = loan['dueDate']

    # appears in /loans/my as ACTIVE
    st, my = call('GET', '/loans/my?size=50', token=token)
    assert loan_id in [l['id'] for l in my['content']], '/loans/my missing new loan'
    print('   /loans/my shows new loan: True')

    # renew once
    st, renewed = call('POST', f'/loans/{loan_id}/renew', token=token)
    print(f'   renew -> {st}, due={renewed["dueDate"]}, renewedCount={renewed["renewedCount"]}')
    assert st == 200, f'renew failed: {renewed}'
    assert renewed['renewedCount'] == 1, 'renewedCount should be 1'
    assert renewed['dueDate'] != due0, 'dueDate should extend after renew'
    due1 = renewed['dueDate']

    # renew again -> should be rejected (max 1)
    st, again = call('POST', f'/loans/{loan_id}/renew', token=token)
    print(f'   renew again -> {st}, body={json.dumps(again, ensure_ascii=False)}')
    assert st != 200, 'second renew should be rejected'
    assert again.get('code') == 'RENEWAL_NOT_ALLOWED', f'unexpected reject code: {again}'

    # return
    st, returned = call('POST', f'/loans/{loan_id}/self-return', token=token)
    print(f'   self-return -> {st}, status={returned["status"]}, copy status={returned.get("copyStatus")}')
    assert st == 200 and returned['status'] == 'RETURNED', f'return failed: {returned}'

    # copy back IN_STOCK
    st, copies2 = call('GET', '/books/1/copies', token=token)
    c2 = next(c for c in copies2 if c['id'] == copy['id'])
    print(f'   copy {copy["id"]} status after return: {c2["status"]}')
    assert c2['status'] == 'IN_STOCK', 'copy should be back IN_STOCK'

    print('\nALL LIFECYCLE CHECKS PASSED: borrow -> renew(1) -> renew-again rejected -> return.')


if __name__ == '__main__':
    main()
