import requests

BASE_URL = "http://localhost:8080"
session = requests.Session()

def setup_auth():
    email = "test_user_5factual@interviewprep.ai"
    session.post(f"{BASE_URL}/api/auth/register", json={
        "name": "Test User 5 Factual Questions",
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

def test_5_factual_questions():
    print("\n=======================================================")
    print("TESTING 5 FACTUAL 'DIFFERENCE BETWEEN X AND Y' QUESTIONS")
    print("=======================================================")
    
    test_cases = [
        {
            "name": "1. Checked vs Unchecked Exceptions",
            "question": "Explain the difference between checked and unchecked exceptions in Java",
            "expected": ["Checked", "Unchecked", "Exception", "RuntimeException", "compile time"],
            "forbidden": ["1. Core Definition: Identify the underlying mechanics", "Contrast runtime performance overhead"]
        },
        {
            "name": "2. ArrayList vs LinkedList",
            "question": "What is the difference between ArrayList and LinkedList in Java?",
            "expected": ["ArrayList", "LinkedList", "array", "random index access", "get(index)"],
            "forbidden": ["1. Core Definition: Identify the underlying mechanics", "Contrast runtime performance overhead"]
        },
        {
            "name": "3. String vs StringBuilder vs StringBuffer",
            "question": "What is the difference between String, StringBuilder, and StringBuffer?",
            "expected": ["String", "StringBuilder", "StringBuffer", "immutable", "thread-safe"],
            "forbidden": ["1. Core Definition: Identify the underlying mechanics", "Contrast runtime performance overhead"]
        },
        {
            "name": "4. Synchronized vs Volatile",
            "question": "What is the difference between synchronized and volatile in Java?",
            "expected": ["synchronized", "volatile", "visibility", "atomicity", "monitor lock"],
            "forbidden": ["1. Core Definition: Identify the underlying mechanics", "Contrast runtime performance overhead"]
        },
        {
            "name": "5. Final vs Finally vs Finalize",
            "question": "What is the difference between final, finally, and finalize in Java?",
            "expected": ["final", "finally", "finalize", "immutable", "cleanup"],
            "forbidden": ["1. Core Definition: Identify the underlying mechanics", "Contrast runtime performance overhead"]
        }
    ]
    
    for tc in test_cases:
        print(f"\n--- Testing {tc['name']} ---")
        payload = {
            "contextType": "PREP",
            "message": tc["question"],
            "targetRole": "Java Developer",
            "difficulty": "MEDIUM"
        }
        resp = session.post(f"{BASE_URL}/api/chat", json=payload)
        assert resp.status_code == 200, f"Failed: {resp.status_code}"
        reply = resp.json().get("reply", "")
        print(f"Question: {tc['question']}\nFull Answer Output:\n{reply}\n")
        
        for exp in tc["expected"]:
            assert exp.lower() in reply.lower(), f"FAILED: '{exp}' not found in reply for '{tc['name']}'"
        for fb in tc["forbidden"]:
            assert fb.lower() not in reply.lower(), f"FAILED: Forbidden abstract template phrase '{fb}' found in reply for '{tc['name']}'"
            
        print(f"[OK] {tc['name']} PASSED with substantive factual content!")

if __name__ == "__main__":
    setup_auth()
    test_5_factual_questions()
    print("\n=======================================================")
    print("ALL 5 FACTUAL QUESTION TESTS VERIFIED AND PASSED SUCCESSFULLY!")
    print("=======================================================")
