import requests

BASE_URL = "http://localhost:8080"
session = requests.Session()

def setup_auth():
    email = "test_user_3issues@interviewprep.ai"
    session.post(f"{BASE_URL}/api/auth/register", json={
        "name": "Test User Three Issues",
        "email": email,
        "password": "Password123!",
        "targetRole": "Java Developer"
    })
    
    login_page = session.get(f"{BASE_URL}/login")
    if 'name="_csrf" value="' in login_page.text:
        csrf = login_page.text.split('name="_csrf" value="')[1].split('"')[0]
    else:
        csrf = ""
    
    login_res = session.post(f"{BASE_URL}/login", data={
        "email": email,
        "password": "Password123!",
        "_csrf": csrf
    })
    print(f"Login status: {login_res.status_code}")
    if csrf:
        session.headers.update({"X-CSRF-TOKEN": csrf})

def test_issue1_prep_technical_question():
    print("\n=======================================================")
    print("TESTING ISSUE 1: PREP Mode Concept Accuracy (@Component, @Service, @Repository)")
    print("=======================================================")
    payload = {
        "contextType": "PREP",
        "message": "What is the difference between @Component, @Service, and @Repository in Spring?",
        "targetRole": "Java Developer",
        "difficulty": "MEDIUM"
    }
    resp = session.post(f"{BASE_URL}/api/chat", json=payload)
    assert resp.status_code == 200, f"Failed: {resp.status_code} - {resp.text}"
    reply = resp.json().get("reply", "")
    print(f"PREP @Component/@Service/@Repository Answer:\n{reply}\n")
    
    assert "@Component" in reply, "Answer missing @Component"
    assert "@Service" in reply, "Answer missing @Service"
    assert "@Repository" in reply, "Answer missing @Repository"
    assert "Spring AOP creates dynamic proxy wrappers" not in reply, "Answer incorrectly returned Spring AOP fallback"
    print("[OK] ISSUE 1 PASSED: @Component, @Service, and @Repository explained accurately without AOP deflection!")

def test_issue2_prep_role_handling():
    print("\n=======================================================")
    print("TESTING ISSUE 2: PREP Mode Role Detection & Acknowledgment")
    print("=======================================================")
    payload = {
        "contextType": "PREP",
        "message": "What should I focus on for a Data Engineer interview?",
        "targetRole": "Java Developer",
        "difficulty": "MEDIUM"
    }
    resp = session.post(f"{BASE_URL}/api/chat", json=payload)
    assert resp.status_code == 200, f"Failed: {resp.status_code} - {resp.text}"
    reply = resp.json().get("reply", "")
    print(f"PREP Data Engineer Question Reply:\n{reply}\n")
    
    assert "Java Developer" in reply, "Answer failed to acknowledge session role Java Developer"
    assert "Data Engineer" in reply, "Answer failed to address requested role Data Engineer"
    assert "Spark" in reply or "data" in reply.lower() or "sql" in reply.lower(), "Answer lacked Data Engineer specific topics"
    print("[OK] ISSUE 2 PASSED: Acknowledged session role Java Developer and provided Data Engineer specific guidance!")

def test_issue3_resume_missing_keywords():
    print("\n=======================================================")
    print("TESTING ISSUE 3: RESUME Mode Missing Keywords Retrieval")
    print("=======================================================")
    # Resume 1: Java developer with missing Docker, Kubernetes, AWS
    resume1_text = "Experienced Software Developer proficient in Java, Spring Boot, REST API, SQL, Hibernate, JUnit, Git."
    jd1_text = "Target Role: Backend Engineer. Requirements: Java, Spring Boot, Docker, Kubernetes, AWS, SQL, REST API."
    
    payload1 = {
        "contextType": "RESUME",
        "message": "How do I fix my missing keywords?",
        "resumeText": resume1_text,
        "jobDescription": jd1_text
    }
    resp1 = session.post(f"{BASE_URL}/api/chat", json=payload1)
    assert resp1.status_code == 200, f"Failed: {resp1.status_code} - {resp1.text}"
    reply1 = resp1.json().get("reply", "")
    print(f"Resume 1 Chat Reply:\n{reply1}\n")
    assert "None identified" not in reply1, "Missing keywords defaulted to None identified for Resume 1"
    assert "Docker" in reply1 or "Kubernetes" in reply1 or "AWS" in reply1, "Missing keywords Docker/Kubernetes/AWS not fetched!"
    
    # Resume 2: Python developer missing Java, Spring Boot, MySQL
    resume2_text = "Python Data Analyst with skills in Pandas, NumPy, Django, PostgreSQL, Git."
    jd2_text = "Senior Java Developer requiring Java, Spring Boot, Unit Testing, MySQL, Docker."
    
    payload2 = {
        "contextType": "RESUME",
        "message": "How do I fix my missing keywords?",
        "resumeText": resume2_text,
        "jobDescription": jd2_text
    }
    resp2 = session.post(f"{BASE_URL}/api/chat", json=payload2)
    assert resp2.status_code == 200, f"Failed: {resp2.status_code} - {resp2.text}"
    reply2 = resp2.json().get("reply", "")
    print(f"Resume 2 Chat Reply:\n{reply2}\n")
    assert "None identified" not in reply2, "Missing keywords defaulted to None identified for Resume 2"
    assert "Java" in reply2 or "Spring" in reply2 or "MySQL" in reply2, "Missing keywords Java/Spring/MySQL not fetched for Resume 2!"
    
    print("[OK] ISSUE 3 PASSED: Both resumes retrieved real distinct missing keywords in RESUME mode!")

if __name__ == "__main__":
    setup_auth()
    test_issue1_prep_technical_question()
    test_issue2_prep_role_handling()
    test_issue3_resume_missing_keywords()
    print("\n=======================================================")
    print("ALL 3 ISSUES VERIFIED AND PASSED SUCCESSFULLY!")
    print("=======================================================")
