import requests

s = requests.Session()
b_url = 'http://localhost:8080'

email = 'fix_verifier@example.com'
s.post(f'{b_url}/api/auth/register', json={
    'name': 'Fix Verifier',
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
print('Login status:', login_res.status_code)
s.headers.update({'X-CSRF-TOKEN': csrf})

print('\n=======================================================')
print('TESTING BUG 1: PREP Mode Direct Concept Answers')
print('=======================================================')

r_tech = s.post(f'{b_url}/api/chat', json={
    'contextType': 'PREP',
    'message': 'Explain the difference between JVM heap and stack memory',
    'targetRole': 'Java Developer'
})
ans_tech = r_tech.json()['reply']
print('Direct Tech Question Reply:\n', ans_tech)

assert 'Heap' in ans_tech or 'Stack' in ans_tech or 'thread' in ans_tech.lower(), 'BUG 1 FAILED: Did not provide substantive JVM heap vs stack explanation!'
assert 'focus on mastering fundamental concepts' not in ans_tech, 'BUG 1 FAILED: Deflected with generic coaching text!'
print('>>> BUG 1 PASSED: PREP mode directly and substantively answered the technical concept question!')

print('\n=======================================================')
print('TESTING BUG 2: REVIEW Mode Question Validation & Out-Of-Bounds')
print('=======================================================')

r_create = s.post(f'{b_url}/api/interviews', json={
    'targetRole': 'Java Developer',
    'interviewType': 'TECHNICAL',
    'difficulty': 'MEDIUM',
    'totalQuestions': 5
})
interview_id = r_create.json()['id']
s.post(f'{b_url}/api/interviews/{interview_id}/start')

# Ask about Question 6 (out of bounds for 5-question session)
r_q6 = s.post(f'{b_url}/api/chat', json={
    'contextType': 'REVIEW',
    'contextId': interview_id,
    'message': "What's the ideal answer to question 6?"
})
ans_q6 = r_q6.json()['reply']
print('REVIEW Question 6 Out-of-Bounds Reply:\n', ans_q6)

assert 'does not exist' in ans_q6.lower() or 'not found' in ans_q6.lower(), 'BUG 2 FAILED: Did not state question 6 does not exist!'
assert 'Question #1' not in ans_q6 and 'Object Encapsulation' not in ans_q6, 'BUG 2 FAILED: Silently returned Question 1 data for Question 6!'
print('>>> BUG 2 PASSED: REVIEW mode explicitly validated question number against session count and rejected out-of-bounds question 6!')

print('\n=======================================================')
print('TESTING BUG 3: RESUME Mode Context & Per-Resume Uniqueness')
print('=======================================================')

r_resA = s.post(f'{b_url}/api/chat', json={
    'contextType': 'RESUME',
    'message': 'How do I fix my missing keywords?',
    'resumeText': 'Built Java microservices with Spring Boot & Redis caching.',
    'jobDescription': 'Looking for Senior Java Developer with Microservices and Docker.',
    'atsScore': 75.0,
    'missingKeywords': ['Docker', 'Kubernetes']
})
ans_resA = r_resA.json()['reply']
print('Resume A Reply:\n', ans_resA)

assert 'As your AI Interview Coach' not in ans_resA, 'BUG 3 FAILED: Returned PREP mode persona!'
assert 'Docker' in ans_resA or 'Kubernetes' in ans_resA or 'ATS' in ans_resA or 'Resume' in ans_resA, 'BUG 3 FAILED: Did not include resume/ATS specific context!'

r_resB = s.post(f'{b_url}/api/chat', json={
    'contextType': 'RESUME',
    'message': 'How do I fix my missing keywords?',
    'resumeText': 'Developed React UI components with TypeScript & Redux state management.',
    'jobDescription': 'Seeking Frontend Engineer with React, Next.js, and GraphQL.',
    'atsScore': 65.0,
    'missingKeywords': ['Next.js', 'GraphQL']
})
ans_resB = r_resB.json()['reply']
print('\nResume B Reply:\n', ans_resB)

assert ans_resA != ans_resB, 'BUG 3 FAILED: Resume A and Resume B produced identical responses!'
assert 'Next.js' in ans_resB or 'GraphQL' in ans_resB or 'React' in ans_resB, 'BUG 3 FAILED: Did not personalize response for Resume B!'
print('>>> BUG 3 PASSED: RESUME mode properly routed and generated per-resume specific advice!')

print('\n=======================================================')
print('TESTING FEATURE: Clear Chat History API')
print('=======================================================')

# Post messages in PREP mode
s.post(f'{b_url}/api/chat', json={'contextType': 'PREP', 'message': 'Hello test 1'})
s.post(f'{b_url}/api/chat', json={'contextType': 'PREP', 'message': 'Hello test 2'})

h_before = s.get(f'{b_url}/api/chat/history?contextType=PREP').json()
print('History count before clear:', len(h_before))
assert len(h_before) >= 2, 'FEATURE FAILED: History count was less than expected!'

del_res = s.delete(f'{b_url}/api/chat/history?contextType=PREP')
print('Delete history status:', del_res.status_code)
assert del_res.status_code == 204, 'FEATURE FAILED: Delete history status was not 204!'

h_after = s.get(f'{b_url}/api/chat/history?contextType=PREP').json()
print('History count after clear:', len(h_after))
assert len(h_after) == 0, 'FEATURE FAILED: History count was not 0 after clear!'
print('>>> FEATURE PASSED: Clear chat history endpoint successfully cleared stored conversation rows!')

print('\n=======================================================')
print('ALL 3 BUGS AND FEATURE VERIFICATION TESTS PASSED SUCCESSFULLY!')
print('=======================================================')
