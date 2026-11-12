"""Export only non-secret fields from real Maven Surefire results."""
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def main():
    suites = []
    for path in sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml')):
        doc = ET.parse(path).getroot()
        properties = {p.get('name'): p.get('value') for p in doc.findall('properties/property')}
        suites.append({'name': doc.get('name'), 'tests': int(doc.get('tests')),
                       'failures': int(doc.get('failures')), 'errors': int(doc.get('errors')),
                       'skipped': int(doc.get('skipped')), 'seconds': float(doc.get('time')),
                       'javaVersion': properties.get('java.version'),
                       'cases': [{'name': c.get('name'), 'passed': all(c.find(t) is None for t in ('failure', 'error', 'skipped'))}
                                 for c in doc.findall('testcase')]})
    if not suites:
        raise SystemExit('No Surefire reports; run tests first.')
    cls = ROOT / 'target/classes/com/bookstore/cloud/CloudApplication.class'
    major = struct.unpack('>H', cls.read_bytes()[6:8])[0]
    report = {'command': 'mvn -o test', 'tests': sum(s['tests'] for s in suites),
              'failures': sum(s['failures'] for s in suites), 'errors': sum(s['errors'] for s in suites),
              'compileRelease': 21, 'classFileMajorVersion': major, 'suites': suites}
    target = ROOT / 'evidence/test-summary.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(f"Saved actual test summary: {report['tests']} tests, {report['failures']} failures, {report['errors']} errors; class major {major}")
    if report['failures'] or report['errors']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
