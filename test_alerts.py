import requests
import sys

sys.stdout.reconfigure(encoding='utf-8')

s = requests.Session()
b_url = 'http://localhost:8080'

email = f'pytest_proctor_grid_{requests.utils.quote("test")}_700@example.com'
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

# 1. Create Interview
r_create = s.post(f'{b_url}/api/interviews', json={
    'targetRole': 'Data Engineer',
    'interviewType': 'TECHNICAL',
    'difficulty': 'MEDIUM',
    'totalQuestions': 5
})
interview_id = r_create.json()['id']
print('Created Interview ID:', interview_id)

# 2. Start Interview
r_start = s.post(f'{b_url}/api/interviews/{interview_id}/start')
print('Start status:', r_start.status_code)

# 3. Post proctoring alerts with Face Mismatch = 1
r_alerts = s.post(f'{b_url}/api/interviews/{interview_id}/proctoring-alerts', json={
    'tabSwitchCount': 0,
    'fullscreenExitCount': 0,
    'externalDeviceCount': 0,
    'noFaceDetectedCount': 0,
    'eyesClosedCount': 0,
    'headTurnedCount': 0,
    'gazeOffScreenCount': 0,
    'multipleFacesCount': 0,
    'faceMismatchCount': 1
})
print('Proctoring Alerts status:', r_alerts.status_code, r_alerts.json())

# 4. Complete interview
r_comp = s.post(f'{b_url}/api/interviews/{interview_id}/complete')
print('Complete status:', r_comp.status_code)

# 5. Fetch report page HTML for THIS newly created interview
r_report = s.get(f'{b_url}/interviews/{interview_id}/report')
print('Report URL:', r_report.url)
print('Report STATUS:', r_report.status_code)

html = r_report.text
print('HTML Length:', len(html))
has_alerts = 'Proctoring Alerts' in html
has_mismatch = 'Face Mismatch' in html
has_tab = 'Tab Switching' in html
has_multiple = 'Multiple Faces' in html

print('Has Proctoring Alerts Header:', has_alerts)
print('Has Face Mismatch:', has_mismatch)
print('Has Tab Switching:', has_tab)
print('Has Multiple Faces:', has_multiple)

if not has_alerts:
    print('--- FIRST 500 CHARS OF HTML ---')
    print(html[:500])


has_behavioral_card = 'Candidate Attention & Behavioral Warnings' in html
print('Has Candidate Attention Card:', has_behavioral_card)

if has_alerts and not has_mismatch and has_multiple and not has_behavioral_card:
    print('=======================================================')
    print('TEST 1 PASSED: Proctoring alerts rendered with Face Mismatch and Behavioral Warnings successfully removed!')
    print('=======================================================')

# 6. Test Mobile Phone Termination Fallback Counter
r_create2 = s.post(f'{b_url}/api/interviews', json={
    'targetRole': 'Data Engineer',
    'interviewType': 'TECHNICAL',
    'difficulty': 'MEDIUM',
    'totalQuestions': 5
})
interview_id2 = r_create2.json()['id']
s.post(f'{b_url}/api/interviews/{interview_id2}/start')

# Terminate with Prohibited Device (Mobile Phone) reason
r_term = s.post(f'{b_url}/api/interviews/{interview_id2}/terminate', json={
    'reason': 'Prohibited Device (Mobile Phone) detected in camera feed',
    'violationCount': 1
})
print('Terminated status:', r_term.status_code)

r_report2 = s.get(f'{b_url}/interviews/{interview_id2}/report')
html2 = r_report2.text

has_external_device_count = 'External devices detected' in html2
print('Mobile Phone Termination Report STATUS:', r_report2.status_code)
print('Has External Devices Detected Label:', has_external_device_count)

if has_external_device_count and 'Prohibited Device (Mobile Phone)' in html2:
    print('=======================================================')
    print('ALL PROCTORING ALERTS & MOBILE DETECTION TESTS PASSED SUCCESSFULLY!')
    print('=======================================================')

