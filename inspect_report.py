import requests

s = requests.Session()
b_url = 'http://localhost:8080'

email = 'pytest_report_inspect_1@example.com'
s.post(f'{b_url}/api/auth/register', json={
    'name': 'Charmi Test',
    'email': email,
    'password': 'Password123!',
    'targetRole': 'Data Engineer'
})

login_page = s.get(f'{b_url}/login')
csrf = login_page.text.split('name="_csrf" value="')[1].split('"')[0]

s.post(f'{b_url}/login', data={
    'email': email,
    'password': 'Password123!',
    '_csrf': csrf
})

r_create = s.post(f'{b_url}/api/interviews', json={
    'targetRole': 'Data Engineer',
    'interviewType': 'TECHNICAL',
    'difficulty': 'MEDIUM',
    'totalQuestions': 5
})
iid = r_create.json()['id']
s.post(f'{b_url}/api/interviews/{iid}/start')

s.post(f'{b_url}/api/interviews/{iid}/proctoring-alerts', json={'faceMismatchCount': 1})
s.post(f'{b_url}/api/interviews/{iid}/complete')

r_api = s.get(f'{b_url}/api/interviews/{iid}/report')
print('API Report status:', r_api.status_code)
print('API Report keys:', list(r_api.json().keys()))
print('faceMismatchCount:', r_api.json().get('faceMismatchCount'))

r_html = s.get(f'{b_url}/interviews/{iid}/report')
print('HTML Report status:', r_html.status_code)

print('Proctoring Alerts :' in r_html.text)
print('Face Mismatch' in r_html.text)

if 'Face Mismatch' in r_html.text:
    print('FOUND FACE MISMATCH IN REPORT HTML!')
