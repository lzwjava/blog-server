#!/usr/bin/env python3
"""
Script to call the create-note API endpoint
"""

import sys
import json
import requests
from typing import Optional

def call_create_note_api(content: str, model: Optional[str] = None) -> dict:
    """
    Call the create-note API endpoint

    Args:
        content: The note content to create
        model: Optional model parameter (defaults to "gpt-4o")

    Returns:
        API response as dictionary
    """
    url = "http://localhost:8080/create-note"

    payload = {"content": content}
    if model:
        payload["model"] = model

    try:
        print(f"Calling API with model: {model or 'gpt-4o'}")
        print(f"Content length: {len(content)} characters")

        response = requests.post(url, json=payload)
        response.raise_for_status()  # Raise exception for bad status codes

        result = {
            "status_code": response.status_code,
            "response": response.text
        }

        print("Success!")
        print(f"Response: {response.text}")

        return result

    except requests.exceptions.RequestException as e:
        error_msg = f"API call failed: {str(e)}"
        print(error_msg, file=sys.stderr)
        return {
            "status_code": getattr(e.response, 'status_code', None) if hasattr(e, 'response') else None,
            "error": error_msg
        }

def main():
    if len(sys.argv) < 2:
        print("Usage: python call_create_note_api.py <content> [model]", file=sys.stderr)
        print("Example: python call_create_note_api.py \"This is my note content\"", file=sys.stderr)
        print("Example: python call_create_note_api.py \"This is my note\" gpt-3.5-turbo", file=sys.stderr)
        sys.exit(1)

    content = sys.argv[1]
    model = sys.argv[2] if len(sys.argv) > 2 else None

    if not content.strip():
        print("Error: Content cannot be empty", file=sys.stderr)
        sys.exit(1)

    result = call_create_note_api(content, model)

    # Exit with non-zero code if there was an error
    if "error" in result:
        sys.exit(1)

if __name__ == "__main__":
    main()