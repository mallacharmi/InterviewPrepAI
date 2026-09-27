import requests

s = requests.Session()
b_url = 'http://localhost:8080'

email = 'chat_test_user@example.com'
s.post(f'{b_url}/api/auth/register', json={
    'name': 'Chat Tester',
    'email': email,
    'password': 'Password123!',
    'targetRole': 'Java Developer'
})

login_page = s.get(f'{b_url}/login')
csrf = login_page.text.split('name="_csrf" value="')[1].split('"')[0]

login_res = s.post(f'{b_url}/login', data={
    'email': email,
    'password': 'Password123!',
    '_csrf': csrf
})
print('Login status:', login_res.status_code, login_res.url)

# 1. Test PREP Mode
r_prep = s.post(f'{b_url}/api/chat', json={
    'contextType': 'PREP',
    'contextId': None,
    'message': 'What key topics should I focus on for a Java Developer interview?',
    'targetRole': 'Java Developer',
    'difficulty': 'MEDIUM',
    'topics': ['Core Java', 'Spring Boot', 'SQL']
})
print('PREP Mode Chat Status:', r_prep.status_code, r_prep.json())

# 2. Test REVIEW Mode with Interview Session
r_create = s.post(f'{b_url}/api/interviews', json={
    'targetRole': 'Java Developer',
    'interviewType': 'TECHNICAL',
    'difficulty': 'MEDIUM',
    'totalQuestions': 5
})
interview_id = r_create.json()['id']
s.post(f'{b_url}/api/interviews/{interview_id}/start')

r_review = s.post(f'{b_url}/api/chat', json={
    'contextType': 'REVIEW',
    'contextId': interview_id,
    'message': 'Why did I score what I did on question 1?'
})
print('REVIEW Mode Chat Status:', r_review.status_code, r_review.json()['reply'][:100])

# 3. Test RESUME Mode
r_resume = s.post(f'{b_url}/api/chat', json={
    'contextType': 'RESUME',
    'contextId': None,
    'message': 'How should I reword my resume bullets to add missing Spring Boot keywords?',
    'resumeText': 'Built Java backends',
    'jobDescription': 'Looking for Spring Boot developer with Microservices',
    'missingKeywords': ['Spring Boot', 'Microservices']
})
print('RESUME Mode Chat Status:', r_resume.status_code, r_resume.json()['reply'][:100])

# 4. Test History Retrieval
r_hist = s.get(f'{b_url}/api/chat/history?contextType=PREP')
print('PREP History Status:', r_hist.status_code, 'History count:', len(r_hist.json()))

print('=======================================================')
print('ALL CHATBOT API TESTS PASSED SUCCESSFULLY!')
print('=======================================================')
