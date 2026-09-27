import requests

BASE_URL = "http://localhost:8080"
session = requests.Session()

def setup_auth():
    email = "test_user_prep_fixes@interviewprep.ai"
    session.post(f"{BASE_URL}/api/auth/register", json={
        "name": "Test User Prep Fixes",
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

def test_issue1_factual_concept_answers():
    print("\n=======================================================")
    print("TESTING ISSUE 1: Factual Concept Questions Answered Directly Without Vague Filler")
    print("=======================================================")
    
    test_cases = [
        {
            "question": "What is the difference between an interface and an abstract class in Java?",
            "expected_keywords": ["Interface", "Abstract Class", "Inheritance", "State"],
            "forbidden_phrases": ["frequently assessed for Java Developer positions", "key architectural trade-offs involve balancing performance"]
        },
        {
            "question": "What is the difference between a Process and a Thread?",
            "expected_keywords": ["Process", "Thread", "Memory Space", "Overhead"],
            "forbidden_phrases": ["frequently assessed", "balancing performance"]
        },
        {
            "question": "What is the difference between REST and gRPC?",
            "expected_keywords": ["REST", "gRPC", "HTTP/2", "Protocol Buffers"],
            "forbidden_phrases": ["frequently assessed", "balancing performance"]
        },
        {
            "question": "What is Garbage Collection in Java?",
            "expected_keywords": ["Garbage Collection", "Heap", "Young Generation", "G1GC"],
            "forbidden_phrases": ["frequently assessed", "balancing performance"]
        }
    ]
    
    for tc in test_cases:
        payload = {
            "contextType": "PREP",
            "message": tc["question"],
            "targetRole": "Java Developer",
            "difficulty": "MEDIUM"
        }
        resp = session.post(f"{BASE_URL}/api/chat", json=payload)
        assert resp.status_code == 200, f"Failed: {resp.status_code}"
        reply = resp.json().get("reply", "")
        print(f"\nQuestion: {tc['question']}\nReply:\n{reply}")
        
        for kw in tc["expected_keywords"]:
            assert kw.lower() in reply.lower(), f"Reply for '{tc['question']}' missing expected keyword '{kw}'"
        for fb in tc["forbidden_phrases"]:
            assert fb.lower() not in reply.lower(), f"Reply for '{tc['question']}' contained forbidden filler phrase '{fb}'"
            
    print("\n[OK] ISSUE 1 PASSED: All 4 factual concept questions answered directly with real technical content!")

def test_issue2_multi_role_content_generation():
    print("\n=======================================================")
    print("TESTING ISSUE 2: PREP Mode Role Mismatch & New-Role Specific Content")
    print("=======================================================")
    
    roles_to_test = [
        {
            "msg": "What should I focus on for a System Architect interview?",
            "requested_role": "System Architect",
            "expected_topics": ["System Architect", "Microservices", "Load Balancer", "Distributed", "Consensus"]
        },
        {
            "msg": "What should I focus on for a DevOps Engineer interview?",
            "requested_role": "DevOps Engineer",
            "expected_topics": ["DevOps Engineer", "CI/CD", "Terraform", "Kubernetes", "Prometheus"]
        },
        {
            "msg": "What should I focus on for a Data Analyst interview?",
            "requested_role": "Data Analyst",
            "expected_topics": ["Data Analyst", "SQL", "Tableau", "Statistical", "Metrics"]
        }
    ]
    
    for r in roles_to_test:
        payload = {
            "contextType": "PREP",
            "message": r["msg"],
            "targetRole": "Java Developer",
            "difficulty": "MEDIUM"
        }
        resp = session.post(f"{BASE_URL}/api/chat", json=payload)
        assert resp.status_code == 200, f"Failed: {resp.status_code}"
        reply = resp.json().get("reply", "")
        print(f"\nQuestion: {r['msg']}\nReply:\n{reply}")
        
        # Must acknowledge original session role Java Developer
        assert "Java Developer" in reply, f"Failed to acknowledge original session role Java Developer in reply for {r['requested_role']}"
        # Must explicitly acknowledge new role
        assert r["requested_role"].lower() in reply.lower(), f"Failed to acknowledge new role {r['requested_role']}"
        
        # Must contain role-specific topics for the NEW role
        matches = [top for top in r["expected_topics"] if top.lower() in reply.lower()]
        assert len(matches) >= 2, f"Reply for {r['requested_role']} lacked specific topics! Found: {matches}"
        
        # Must NOT fall back to generic Java Developer advice when asking about new role
        assert "framework fundamentals" not in reply or r["requested_role"] == "Java Developer", f"Incorrectly returned generic Java Developer advice for {r['requested_role']}"

    print("\n[OK] ISSUE 2 PASSED: All 3 new roles acknowledged session role and generated new-role specific content!")

if __name__ == "__main__":
    setup_auth()
    test_issue1_factual_concept_answers()
    test_issue2_multi_role_content_generation()
    print("\n=======================================================")
    print("ALL PREP FIXES VERIFIED AND PASSED SUCCESSFULLY!")
    print("=======================================================")
