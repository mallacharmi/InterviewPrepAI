import requests

BASE_URL = "http://localhost:8080"

def create_user_and_login(name, email, password):
    s = requests.Session()
    s.post(f"{BASE_URL}/api/auth/register", json={
        "name": name,
        "email": email,
        "password": password,
        "targetRole": "Java Developer"
    })
    
    login_page = s.get(f"{BASE_URL}/login")
    if 'name="_csrf" value="' in login_page.text:
        csrf = login_page.text.split('name="_csrf" value="')[1].split('"')[0]
    else:
        csrf = ""
    
    res = s.post(f"{BASE_URL}/login", data={
        "email": email,
        "password": password,
        "_csrf": csrf
    })
    if csrf:
        s.headers.update({"X-CSRF-TOKEN": csrf})
    print(f"Logged in as {email}: HTTP {res.status_code}")
    return s

def test_idor_security():
    print("\n=======================================================")
    print("TESTING IDOR BROKEN ACCESS CONTROL VULNERABILITY FIX")
    print("=======================================================")
    
    sessionA = create_user_and_login("Candidate A", "candidateA_idor@interviewprep.ai", "Password123!")
    sessionB = create_user_and_login("Candidate B", "candidateB_idor@interviewprep.ai", "Password123!")
    
    # 1. Candidate A creates an interview session
    create_res = sessionA.post(f"{BASE_URL}/api/interviews", json={
        "targetRole": "Java Developer",
        "interviewType": "TECHNICAL",
        "difficulty": "MEDIUM",
        "totalQuestions": 5
    })
    assert create_res.status_code == 201, f"Failed to create interview for A: {create_res.status_code}"
    interview_id = create_res.json()["id"]
    print(f"Candidate A created Interview Session ID: {interview_id}")
    
    # Start and complete interview for Candidate A
    sessionA.post(f"{BASE_URL}/api/interviews/{interview_id}/start")
    sessionA.post(f"{BASE_URL}/api/interviews/{interview_id}/complete")
    
    # 2. Verify Candidate A CAN access their own report & interview
    resA_report = sessionA.get(f"{BASE_URL}/interviews/{interview_id}/report")
    assert resA_report.status_code == 200, f"Candidate A should have access to own report: {resA_report.status_code}"
    print(f"[OK] Candidate A successfully accessed their own report card: HTTP {resA_report.status_code}")
    
    # 3. Candidate B attempts IDOR attacks on Candidate A's interview ID
    print(f"\n--- Testing Candidate B unauthorized IDOR access on Interview ID {interview_id} ---")
    
    idor_endpoints = [
        ("GET /interviews/{id}/report (Report Page)", lambda: sessionB.get(f"{BASE_URL}/interviews/{interview_id}/report")),
        ("GET /interviews/{id} (Live Interview Room)", lambda: sessionB.get(f"{BASE_URL}/interviews/{interview_id}")),
        ("GET /interviews/{id}/detail (Detail Page)", lambda: sessionB.get(f"{BASE_URL}/interviews/{interview_id}/detail")),
        ("GET /api/interviews/{id} (API Interview Metadata)", lambda: sessionB.get(f"{BASE_URL}/api/interviews/{interview_id}")),
        ("GET /api/interviews/{id}/report (API Report Data)", lambda: sessionB.get(f"{BASE_URL}/api/interviews/{interview_id}/report")),
        ("POST /api/interviews/{id}/start (Start Session)", lambda: sessionB.post(f"{BASE_URL}/api/interviews/{interview_id}/start")),
        ("GET /api/interviews/{id}/question (Current Question)", lambda: sessionB.get(f"{BASE_URL}/api/interviews/{interview_id}/question")),
        ("POST /api/interviews/{id}/complete (Complete Session)", lambda: sessionB.post(f"{BASE_URL}/api/interviews/{interview_id}/complete")),
        ("POST /api/interviews/{id}/terminate (Terminate Session)", lambda: sessionB.post(f"{BASE_URL}/api/interviews/{interview_id}/terminate")),
        ("DELETE /api/interviews/{id} (Delete Session)", lambda: sessionB.delete(f"{BASE_URL}/api/interviews/{interview_id}")),
        ("GET /api/interviews/{id}/video (Video Recording Playback)", lambda: sessionB.get(f"{BASE_URL}/api/interviews/{interview_id}/video")),
        ("POST /api/interviews/{id}/expressions (Facial Expressions)", lambda: sessionB.post(f"{BASE_URL}/api/interviews/{interview_id}/expressions", json={})),
        ("POST /api/chat (REVIEW Chatbot Mode)", lambda: sessionB.post(f"{BASE_URL}/api/chat", json={"contextType": "REVIEW", "contextId": interview_id, "message": "What is Q1 score?"})),
        ("GET /api/chat/history (REVIEW Chat History)", lambda: sessionB.get(f"{BASE_URL}/api/chat/history?contextType=REVIEW&contextId={interview_id}")),
    ]
    
    passed_security_count = 0
    for name, test_fn in idor_endpoints:
        res = test_fn()
        status = res.status_code
        print(f"IDOR Check '{name}': HTTP {status}")
        assert status == 403 or status == 404, f"SECURITY VIOLATION! Candidate B accessed Candidate A's resource via {name}: HTTP {status}"
        passed_security_count += 1
        
    print(f"\n[OK] ALL {passed_security_count} IDOR ENDPOINTS REJECTED UNAUTHORIZED ACCESS WITH HTTP 403 FORBIDDEN!")
    print("=======================================================")
    print("ALL BROKEN ACCESS CONTROL VULNERABILITY TESTS PASSED!")
    print("=======================================================")

if __name__ == "__main__":
    test_idor_security()
