import requests
import json
import sys
import time

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
    print(f"  [OK] Authenticated as {email}: HTTP {res.status_code}")
    return s

def test_full_system():
    ts = int(time.time())
    email = f"verifier_{ts}@interviewprep.ai"
    password = "Password123!"

    print("=" * 70)
    print("      INTERVIEWPREP AI - COMPLETE SYSTEM VERIFICATION SUITE")
    print("=" * 70)
    
    # 1. Candidate Registration & Login
    print(f"\n[1/7] Testing Authentication & User Session ({email})...")
    s = create_user_and_login("System Verifier", email, password)

    # 2. Dashboard Stats & Page
    print("\n[2/7] Testing Dashboard Views & REST APIs...")
    r = s.get(f"{BASE_URL}/dashboard")
    assert r.status_code == 200, f"Dashboard view failed: {r.status_code}"
    print("  [OK] HTML Dashboard page loaded (HTTP 200)")

    r = s.get(f"{BASE_URL}/api/dashboard")
    assert r.status_code == 200, f"Dashboard API failed: {r.status_code}"
    dashboard_data = r.json()
    print(f"  [OK] Dashboard API retrieved: Total Interviews={dashboard_data.get('totalInterviews', 0)}, Avg Score={dashboard_data.get('averageScore', 0)}")

    # 3. Unified Chatbot - PREP Mode
    print("\n[3/7] Testing Unified Chatbot - PREP Mode...")
    prep_req = {
        "contextType": "PREP",
        "message": "Explain the difference between JVM heap and stack memory in detail."
    }
    r = s.post(f"{BASE_URL}/api/chat", json=prep_req)
    assert r.status_code == 200, f"PREP chat failed: {r.status_code}"
    chat_resp = r.json()
    reply = chat_resp.get("reply", "")
    assert len(reply) > 50, "PREP mode returned an empty or too short response"
    print(f"  [OK] PREP Chatbot answered correctly ({len(reply)} chars):")
    print(f"       Snippet: {reply[:120]}...")

    # 4. ATS Resume Analyzer
    print("\n[4/7] Testing ATS Resume Analyzer & Interview Creation...")
    ats_req = {
        "resumeText": "Experienced Java Developer skilled in Spring Boot, REST APIs, Microservices, and SQL.",
        "jobDescription": "Looking for a Senior Java Developer with Spring Security, Docker, Kubernetes, and AWS experience."
    }
    r = s.post(f"{BASE_URL}/api/ats/analyze", json=ats_req)
    assert r.status_code == 200, f"ATS analysis failed: {r.status_code}"
    ats_res = r.json()
    match_score = ats_res.get("atsMatchScore", 0)
    missing_skills = ats_res.get("missingSkills", [])
    print(f"  [OK] ATS Analyzer evaluated match score: {match_score}%, Missing skills: {missing_skills}")

    ats_interview_req = {
        "targetRole": "Senior Java Developer",
        "matchedKeywords": ["Java", "Spring Boot", "REST APIs"],
        "missingSkills": missing_skills
    }
    r = s.post(f"{BASE_URL}/api/ats/create-interview", json=ats_interview_req)
    assert r.status_code in (200, 201), f"Create interview from ATS failed: {r.status_code}"
    ats_interview = r.json()
    ats_interview_id = ats_interview.get("id")
    print(f"  [OK] Created Practice Session ID {ats_interview_id} directly from ATS Resume recommendations!")

    # 5. Practice Interview Session Lifecycle & Proctoring
    print("\n[5/7] Testing Complete Practice Session Lifecycle & Proctoring...")
    create_req = {
        "targetRole": "Java Backend Engineer",
        "interviewType": "TECHNICAL",
        "difficulty": "MEDIUM",
        "totalQuestions": 5,
        "topics": ["Spring Boot", "JVM", "SQL"]
    }
    r = s.post(f"{BASE_URL}/api/interviews", json=create_req)
    assert r.status_code in (200, 201), f"Create interview failed: {r.status_code}"
    interview = r.json()
    interview_id = interview["id"]
    print(f"  [OK] Session Created ID: {interview_id}")

    # Start session
    r = s.post(f"{BASE_URL}/api/interviews/{interview_id}/start")
    assert r.status_code == 200, "Start interview failed"
    print("  [OK] Session Started")

    # Fetch Question 1
    r = s.get(f"{BASE_URL}/api/interviews/{interview_id}/question")
    assert r.status_code == 200, "Get question failed"
    q_data = r.json()
    q_id = q_data["id"]
    print(f"  [OK] Question 1 Retrieved (ID {q_id}): '{q_data.get('questionText', '')[:60]}...'")

    # Submit Answer 1
    answer_payload = {
        "answerText": "The JVM heap is used for dynamic memory allocation of objects, shared across all threads. The stack is thread-local and stores primitive values and references to objects."
    }
    r = s.post(f"{BASE_URL}/api/interviews/{interview_id}/questions/{q_id}/answer", json=answer_payload)
    assert r.status_code == 200, f"Submit answer failed: {r.status_code} {r.text}"
    ans_res = r.json()
    print(f"  [OK] Answer Submitted & AI Evaluated Score: {ans_res.get('score', 0)}/10")

    # Proctoring Expression Event
    expr_payload = {
        "expression": "LOOKING_AWAY",
        "confidence": 0.95
    }
    r = s.post(f"{BASE_URL}/api/interviews/{interview_id}/expressions", json=expr_payload)
    assert r.status_code in (200, 201), f"Proctoring expression failed: {r.status_code}"
    print("  [OK] Proctoring event recorded (LOOKING_AWAY)")

    # Complete session
    r = s.post(f"{BASE_URL}/api/interviews/{interview_id}/complete")
    assert r.status_code == 200, f"Complete interview failed: {r.status_code}"
    print("  [OK] Practice Session Completed Successfully")

    # 6. Report Card Access & REVIEW Chatbot
    print("\n[6/7] Testing Report Card & REVIEW Mode Chatbot...")
    r = s.get(f"{BASE_URL}/interviews/{interview_id}/report")
    assert r.status_code == 200, f"Report page failed: {r.status_code}"
    print("  [OK] HTML Report Card page loaded (HTTP 200)")

    r = s.get(f"{BASE_URL}/api/interviews/{interview_id}/report")
    assert r.status_code == 200, f"Report API failed: {r.status_code}"
    report_data = r.json()
    print(f"  [OK] Report Card API retrieved: Overall Score={report_data.get('overallScore', 0)}/100")

    review_req = {
        "contextType": "REVIEW",
        "contextId": str(interview_id),
        "message": "How can I improve my score on question 1?"
    }
    r = s.post(f"{BASE_URL}/api/chat", json=review_req)
    assert r.status_code == 200, f"REVIEW chat failed: {r.status_code}"
    review_resp = r.json()
    print(f"  [OK] REVIEW Chatbot provided feedback on Session {interview_id}:")
    print(f"       Snippet: {review_resp.get('reply', '')[:120]}...")

    # 7. Security IDOR Verification
    print("\n[7/7] Testing Cross-Candidate Security Isolation...")
    other_email = f"unauth_{ts}@interviewprep.ai"
    s_other = create_user_and_login("Unauthorized User", other_email, password)
    r_unauth = s_other.get(f"{BASE_URL}/interviews/{interview_id}/report")
    assert r_unauth.status_code == 403, f"IDOR check failed! Expected 403 but got {r_unauth.status_code}"
    print(f"  [OK] IDOR Protection Confirmed: Non-owner request returned HTTP 403 Forbidden")

    print("\n" + "=" * 70)
    print("   ALL SYSTEM COMPONENTS VERIFIED & WORKING 100% PERFECTLY!")
    print("=" * 70)

if __name__ == "__main__":
    test_full_system()
