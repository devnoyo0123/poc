#!/usr/bin/env python3
"""
Claude + ArchUnit Auto-Fix Automation Script

This script implements the workflow:
1. Run ArchUnit tests
2. If tests pass → Success
3. If tests fail → Parse violations, send to Claude
4. Claude generates fix
5. Apply fix and re-run tests (loop)
"""

import os
import sys
import subprocess
import json
import re
from pathlib import Path
from typing import List, Optional

# Anthropic API Configuration
API_KEY = os.getenv("ANTHROPIC_AUTH_TOKEN", "c8d1a5f6fa2a42579e897dff02dd40d4.C8i30rGE0kGqTNia")
API_URL = os.getenv("ANTHROPIC_BASE_URL", "https://api.z.ai/api/anthropic")

PROJECT_ROOT = Path(__file__).parent.parent
TEST_PROJECT = PROJECT_ROOT / "test-project"
ARCHUNIT_PROJECT = TEST_PROJECT  # 단일 프로젝트 구조로 수정


class Colors:
    """ANSI color codes for terminal output"""
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    RESET = '\033[0m'


def log(message: str, color: str = Colors.RESET):
    """Print colored message"""
    print(f"{color}{message}{Colors.RESET}")


def run_gradle_test(project_dir: Path) -> tuple[bool, str]:
    """
    Run Gradle tests and return (success, output)

    Returns:
        (bool): True if all tests passed
        (str): Full test output
    """
    log(f"\n🧪 Running tests in {project_dir.name}...", Colors.BLUE)

    result = subprocess.run(
        ["./gradlew", "test", "--console=plain"],
        cwd=project_dir,
        capture_output=True,
        text=True
    )

    output = result.stdout + result.stderr
    success = result.returncode == 0

    return success, output


def parse_archunit_violations(test_output: str) -> List[str]:
    """
    Parse ArchUnit violation messages from test output

    Returns list of violation descriptions
    """
    violations = []

    # ArchUnit violations typically follow this pattern:
    # Architecture Violation: [description]
    # or
    # java.lang.AssertionError: Architecture Violation ...
    # or test method names with FAILED status

    # First try to extract from error output
    lines = test_output.split('\n')

    for i, line in enumerate(lines):
        # Look for test failures
        if 'FAILED' in line and 'ArchitectureTest' in line:
            # Found a failing test, extract context
            violation = {
                'test': line.strip(),
                'details': []
            }

            # Get next few lines for context
            for j in range(i+1, min(i+10, len(lines))):
                if lines[j].strip():
                    violation['details'].append(lines[j].strip())
                elif lines[j].strip().startswith('>'):
                    break

            violations.append(violation)

        # Look for explicit violation messages
        if 'Violation' in line or 'violated' in line.lower():
            violations.append({
                'message': line.strip(),
                'details': []
            })

    return violations


def read_source_files() -> dict[str, str]:
    """Read all Java source files"""
    sources = {}

    for java_file in TEST_PROJECT.rglob("*.java"):
        relative_path = java_file.relative_to(TEST_PROJECT)
        sources[str(relative_path)] = java_file.read_text()

    return sources


def call_claude_api(violations: List[str], sources: dict[str, str]) -> Optional[dict]:
    """
    Send violations and source code to Claude API for auto-fix

    Returns:
        dict with 'files_to_modify' and 'new_content' keys
    """
    log("\n🤖 Sending violations to Claude for analysis...", Colors.YELLOW)

    # Prepare prompt
    violations_text = "\n".join(f"- {v}" for v in violations)
    sources_text = "\n\n".join(f"### {path}\n```\n{code}\n```"
                                   for path, code in sources.items())

    prompt = f"""You are an architecture fix expert. The following ArchUnit test violations were detected:

{violations_text}

Current source files:
{sources_text}

Please analyze these violations and generate the fixed code. Return your response as a JSON object with this exact structure:
{{
  "analysis": "brief explanation of what violates architecture rules",
  "files_to_modify": [
    {{
      "file": "path/to/File.java",
      "changes": "description of changes needed",
      "new_content": "complete new file content with fixes applied"
    }}
  ]
}}

Only output the JSON object, nothing else."""

    try:
        import urllib.request
        import urllib.error

        headers = {
            "x-api-key": API_KEY,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json"
        }

        data = json.dumps({
            "model": "claude-sonnet-4-5-20250929",
            "max_tokens": 4096,
            "messages": [
                {
                    "role": "user",
                    "content": prompt
                }
            ]
        }).encode()

        req = urllib.request.Request(
            f"{API_URL}/v1/messages",
            data=data,
            headers=headers,
            method="POST"
        )

        with urllib.request.urlopen(req, timeout=120) as response:
            response_data = json.loads(response.read().decode())

        # Extract Claude's response
        content = response_data.get("content", [{}])[0].get("text", "")

        # Parse JSON from Claude's response
        json_match = re.search(r'\{.*\}', content, re.DOTALL)
        if json_match:
            return json.loads(json_match.group(0))

        log("⚠️ Claude response didn't contain valid JSON", Colors.RED)
        log(f"Response: {content[:200]}...", Colors.RED)
        return None

    except Exception as e:
        log(f"❌ Error calling Claude API: {e}", Colors.RED)
        return None


def apply_fixes(fix_data: dict) -> bool:
    """
    Apply Claude's suggested fixes to source files

    Returns:
        bool: True if all files were successfully updated
    """
    files_to_modify = fix_data.get("files_to_modify", [])

    if not files_to_modify:
        log("⚠️ No files to modify in Claude's response", Colors.YELLOW)
        return False

    log(f"\n🔧 Applying {len(files_to_modify)} fix(es)...", Colors.BLUE)

    for file_info in files_to_modify:
        file_path = TEST_PROJECT / file_info["file"]
        new_content = file_info["new_content"]

        log(f"  • Modifying {file_info['file']}", Colors.BLUE)
        log(f"    {file_info['changes']}", Colors.RESET)

        # Backup original
        if file_path.exists():
            backup_path = file_path.with_suffix(file_path.suffix + ".bak")
            file_path.rename(backup_path)
            log(f"    Backed up to {backup_path.name}", Colors.GREEN)

        # Write new content
        file_path.parent.mkdir(parents=True, exist_ok=True)
        file_path.write_text(new_content)

    return True


def main():
    """Main workflow loop"""
    log("="*60, Colors.BLUE)
    log("Claude + ArchUnit Auto-Fix Workflow", Colors.BLUE)
    log("="*60, Colors.BLUE)

    max_iterations = 5
    iteration = 0

    while iteration < max_iterations:
        iteration += 1
        log(f"\n📍 Iteration {iteration}/{max_iterations}", Colors.BLUE)

        # Step 1: Run ArchUnit tests
        success, output = run_gradle_test(ARCHUNIT_PROJECT)

        if success:
            log("\n✅ All architecture tests passed!", Colors.GREEN)
            log("   Architecture rules are satisfied.", Colors.GREEN)
            return 0

        # Step 2: Parse violations
        violations = parse_archunit_violations(output)

        if not violations:
            log("\n⚠️ Tests failed but couldn't parse violations", Colors.YELLOW)
            log("   Check test output manually:", Colors.YELLOW)
            log(output[:500], Colors.RESET)
            return 1

        log(f"\n❌ Found {len(violations)} architecture violation(s)", Colors.RED)
        for i, violation in enumerate(violations, 1):
            if isinstance(violation, dict):
                if 'test' in violation:
                    log(f"   {i}. {violation['test']}", Colors.RED)
                    for detail in violation.get('details', [])[:3]:  # Show first 3 details
                        log(f"      {detail}", Colors.RED)
                else:
                    log(f"   {i}. {violation.get('message', 'Unknown violation')}", Colors.RED)
            else:
                log(f"   {i}. {violation}", Colors.RED)

        # Step 3: Call Claude API for fixes
        sources = read_source_files()
        fix_data = call_claude_api(violations, sources)

        if not fix_data:
            log("\n❌ Failed to get fix from Claude", Colors.RED)
            return 1

        # Step 4: Display analysis
        analysis = fix_data.get("analysis", "")
        if analysis:
            log(f"\n📋 Claude's Analysis:", Colors.YELLOW)
            log(f"   {analysis}", Colors.RESET)

        # Step 5: Apply fixes
        if not apply_fixes(fix_data):
            log("\n❌ Failed to apply fixes", Colors.RED)
            return 1

        log("\n✅ Fixes applied, re-running tests...", Colors.GREEN)

    # Max iterations reached
    log(f"\n⚠️ Reached maximum iterations ({max_iterations})", Colors.YELLOW)
    log("   Please review manually or investigate recurring issues.", Colors.YELLOW)
    return 1


if __name__ == "__main__":
    sys.exit(main())
