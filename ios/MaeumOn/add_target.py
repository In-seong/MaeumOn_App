#!/usr/bin/env python3
"""
Kwangdong 타겟을 MobiliteasyBase 프로젝트에 추가하는 스크립트
MobiliteasyLite 타겟을 복제하여 생성합니다.
"""

import re
import sys

# UUID 매핑 (Lite -> Kwangdong)
UUID_MAPPING = {
    '8A0AE0B82EE841D000B18434': '8AKWANG02EE841D000B18434',  # Target
    '8A0AE0CD2EE841D000B18434': '8AKWANGD2EE841D000B18434',  # App Reference
    '8A0AE0BF2EE841D000B18434': '8AKWANGF2EE841D000B18434',  # Check Pods
    '8A0AE0C02EE841D000B18434': '8AKWANG02EE841D000B18435',  # Sources
    '8A0AE0C12EE841D000B18434': '8AKWANG12EE841D000B18434',  # Frameworks
    '8A0AE0C82EE841D000B18434': '8AKWANG82EE841D000B18434',  # Resources
    '8A0AE0C92EE841D000B18434': '8AKWANG92EE841D000B18434',  # Embed Pods
    '8A0AE0D32EE8456200B18434': '8AKWANGD3EE8456200B18434',  # Run Script (GoogleService)
    '8A0AE0CA2EE841D000B18434': '8AKWANGCA2EE841D000B18434',  # Build Config List
    '8A0AE0CB2EE841D000B18434': '8AKWANGCB2EE841D000B18434',  # Debug Config
    '8A0AE0CC2EE841D000B18434': '8AKWANGCC2EE841D000B18434',  # Release Config
    '8A0AE0B92EE841D000B18434': '8AKWANGB92EE841D000B18434',  # Firebase Analytics
    '8A0AE0BB2EE841D000B18434': '8AKWANGBB2EE841D000B18434',  # Firebase Core
    '8A0AE0BC2EE841D000B18434': '8AKWANGBC2EE841D000B18434',  # Firebase Messaging
    '8A0AE0BD2EE841D000B18434': '8AKWANGBD2EE841D000B18434',  # DeviceKit
    '8A0AE0BA2EE841D000B18434': '8AKWANGBA2EE841D000B18434',  # Firebase Package
    '8A0AE0BE2EE841D000B18434': '8AKWANGBE2EE841D000B18434',  # DeviceKit Package
    '8A0AE0CE2EE841D000B18434': '8AKWANGCE2EE841D000B18434',  # Exception Set
    '8A0AE0CF2EE841D000B18434': '8AKWANGCF2EE841D000B18434',  # Info.plist ref
    '699776F432AF33B21B90C799': '699KWG F432AF33B21B90C799',  # Pods debug xcconfig
    '6DB9E36ED1A1446DC379F45F': '6DBKWG6ED1A1446DC379F45F',  # Pods release xcconfig
    '50946BC7045F8987DAE2D969': '50KWGBC7045F8987DAE2D969',  # Pods framework
}

def replace_uuids(text, mapping):
    """UUID를 매핑에 따라 치환"""
    for old_uuid, new_uuid in mapping.items():
        text = text.replace(old_uuid, new_uuid)
    return text

def main():
    project_file = 'MobiliteasyBase.xcodeproj/project.pbxproj'

    print("📖 프로젝트 파일 읽는 중...")
    with open(project_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Lite 타겟 관련 섹션 찾기
    print("🔍 MobiliteasyLite 타겟 찾는 중...")

    # Lite 관련 섹션 추출
    lite_sections = []

    # 1. PBXBuildFile 섹션에서 Lite 관련 항목
    buildfile_pattern = r'(8A0AE0C[0-9]2EE841D000B18434[^\n]+Frameworks[^\n]+\n)'
    for match in re.finditer(buildfile_pattern, content):
        lite_sections.append(('PBXBuildFile', match.group(0)))

    # 2. PBXFileReference에서 Lite 관련 항목
    fileref_pattern = r'(8A0AE0[^\s]+\s+/\*[^*]+\*/[^;]+;)'
    for match in re.finditer(fileref_pattern, content):
        if 'Lite' in match.group(0) or '8A0AE0' in match.group(0):
            lite_sections.append(('PBXFileReference', match.group(0)))

    print(f"✅ {len(lite_sections)}개 섹션 발견")

    # UUID 치환하여 Kwangdong 섹션 생성
    print("🔄 Kwangdong 타겟 생성 중...")
    kwangdong_sections = []
    for section_type, section_content in lite_sections:
        kwangdong_content = replace_uuids(section_content, UUID_MAPPING)
        kwangdong_content = kwangdong_content.replace('Lite', 'Kwangdong')
        kwangdong_content = kwangdong_content.replace('MobiliteasyLite', 'KwangdongCustomer')
        kwangdong_content = kwangdong_content.replace('mobiliteasylite', 'kwangdongcustomer')
        kwangdong_sections.append((section_type, kwangdong_content))

    print(f"✅ {len(kwangdong_sections)}개 섹션 생성 완료")
    print("⚠️  수동 작업이 필요합니다.")
    print("    Xcode에서 MobiliteasyLite를 Duplicate하는 것을 권장합니다.")

    return 0

if __name__ == '__main__':
    sys.exit(main())
