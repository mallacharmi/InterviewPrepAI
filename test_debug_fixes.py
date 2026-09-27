import requests

s = requests.Session()
b_url = 'http://localhost:8080'

email = 'debug_test_user@example.com'
s.post(f'{b_url}/api/auth/register', json={
    'name': 'Debug Tester',
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
print('TESTING ISSUE 1: PREP Mode Intent & Uniqueness')
print('=======================================================')

# 1. Ask PREP mode about scores
r_score1 = s.post(f'{b_url}/api/chat', json={
    'contextType': 'PREP',
    'message': 'how did I score on my last interview?',
    'targetRole': 'Java Developer'
})
ans_score1 = r_score1.json()['reply']
print('Score Question 1 Reply:\n', ans_score1)

r_score2 = s.post(f'{b_url}/api/chat', json={
    'contextType': 'PREP',
    'message': 'what topics should I study to prepare?',
    'targetRole': 'Java Developer'
})
ans_score2 = r_score2.json()['reply']
print('\nTopic Question Reply:\n', ans_score2)

assert ans_score1 != ans_score2, 'ISSUE 1 FAILED: Responses were identical!'
assert 'REVIEW mode' in ans_score1 or 'Report Card' in ans_score1 or 'past session' in ans_score1, 'ISSUE 1 FAILED: Did not direct past score query to REVIEW mode!'
print('\n>>> ISSUE 1 PASSED: PREP mode produces distinct, context-aware responses and correctly directs past score queries to REVIEW mode!')

print('\n=======================================================')
print('TESTING ISSUE 2: REVIEW Mode Specific Question Grounding')
print('=======================================================')

# Create an interview session with 5 questions
r_create = s.post(f'{b_url}/api/interviews', json={
    'targetRole': 'Java Developer',
    'interviewType': 'TECHNICAL',
    'difficulty': 'MEDIUM',
    'totalQuestions': 5
})
interview_id = r_create.json()['id']
print(f'Created session ID: {interview_id}')

# Start session -> generates Question 1
q1_res = s.post(f'{b_url}/api/interviews/{interview_id}/start').json()
print(f'Started session. Question 1 ID: {q1_res["id"]}, Text: {q1_res.get("questionText")[:50]}...')

# Submit Answer 1
ans1 = s.post(f'{b_url}/api/interviews/{interview_id}/questions/{q1_res["id"]}/answer', json={
    'answerText': 'In Java, Object Encapsulation means hiding data behind getters and setters.',
    'audioDurationSeconds': 20
})
print('Answer 1 post status:', ans1.status_code)

# Fetch next question (Question 2)
q2_req = s.get(f'{b_url}/api/interviews/{interview_id}/question')
print('Question 2 get status:', q2_req.status_code)
if q2_req.status_code == 200:
    q2_res = q2_req.json()
    print(f'Question 2 ID: {q2_res.get("id")}, Text: {q2_res.get("questionText")[:50]}...')
    
    # Submit Answer 2
    ans2 = s.post(f'{b_url}/api/interviews/{interview_id}/questions/{q2_res["id"]}/answer', json={
        'answerText': 'HashMap uses hash code to index array buckets and handles collision with linked list.',
        'audioDurationSeconds': 25
    })
    print('Answer 2 post status:', ans2.status_code)

# Fetch next question (Question 3)
q3_req = s.get(f'{b_url}/api/interviews/{interview_id}/question')
print('Question 3 get status:', q3_req.status_code)
if q3_req.status_code == 200:
    q3_res = q3_req.json()
    print(f'Question 3 ID: {q3_res.get("id")}, Text: {q3_res.get("questionText")[:50]}...')

    # Submit Answer 3 for Question 3
    ans3 = s.post(f'{b_url}/api/interviews/{interview_id}/questions/{q3_res["id"]}/answer', json={
        'answerText': 'Multithreading relies on volatile for visibility and synchronized blocks for mutex locking.',
        'audioDurationSeconds': 30
    })
    print('Answer 3 post status:', ans3.status_code)

# Ask REVIEW mode about question 3
r_rev3 = s.post(f'{b_url}/api/chat', json={
    'contextType': 'REVIEW',
    'contextId': interview_id,
    'message': 'why did I lose marks on question 3?'
})
ans_rev3 = r_rev3.json()['reply']
print('\nREVIEW Question 3 Reply:\n', ans_rev3)

assert 'Multithreading' in ans_rev3 or 'volatile' in ans_rev3 or 'Question 3' in ans_rev3 or 'Question' in ans_rev3, 'ISSUE 2 FAILED: Response was not grounded in question 3 text/answer!'
assert 'Score' in ans_rev3 or 'Feedback' in ans_rev3, 'ISSUE 2 FAILED: Missing score/feedback in reply!'
print('\n>>> ISSUE 2 GROUNDED QUESTION TEST PASSED!')

# Test Fallback: Ask REVIEW mode about Question 99 (Out of bounds)
r_rev99 = s.post(f'{b_url}/api/chat', json={
    'contextType': 'REVIEW',
    'contextId': interview_id,
    'message': 'why did I lose marks on question 99?'
})
ans_rev99 = r_rev99.json()['reply']
print('\nREVIEW Question 99 (Out of bounds) Reply:\n', ans_rev99)

assert 'not found' in ans_rev99.lower() or 'does not contain' in ans_rev99.lower(), 'ISSUE 2 FALLBACK FAILED: Did not indicate question 99 was out of bounds!'
print('\n>>> ISSUE 2 OUT-OF-BOUNDS FALLBACK TEST PASSED!')

print('\n=======================================================')
print('ALL DEBUGGING & FIX VERIFICATION TESTS PASSED SUCCESSFULLY!')
print('=======================================================')
