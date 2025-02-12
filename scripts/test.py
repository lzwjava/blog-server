import requests

response = requests.get("http://localhost:8080")
print(response.text)
