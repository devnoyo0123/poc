#!/usr/bin/env python3
"""
대용량 엑셀 파일 생성 스크립트
EasyExcel SAX streaming 성능 테스트용
"""

from openpyxl import Workbook
import random

def generate_large_excel(filename, row_count):
    """대용량 엑셀 파일 생성"""

    wb = Workbook()
    ws = wb.active
    ws.title = "Sheet1"

    # 헤더 행
    headers = ["저작권료", "저작자", "작품명", "사용일", "판매량"]
    ws.append(headers)

    # 데이터 행 생성
    print(f"{row_count:,}행 데이터 생성 중...")

    for i in range(1, row_count + 1):
        row_data = [
            random.randint(10000, 500000),  # 저작권료
            f"저작자_{random.randint(1, 100)}",  # 저작자
            f"작품명_{random.randint(1, 500)}",  # 작품명
            f"2024-{random.randint(1, 12):02d}-{random.randint(1, 28):02d}",  # 사용일
            random.randint(100, 10000)  # 판매량
        ]
        ws.append(row_data)

        if i % 10000 == 0:
            print(f"  {i:,}행 완료...")

    wb.save(filename)
    print(f"\n✅ {filename} 생성 완료: {row_count:,}행")

if __name__ == "__main__":
    # 10,000행 파일 생성
    generate_large_excel("samples/type_a_large_10k.xlsx", 10000)

    # 50,000행 파일 생성
    generate_large_excel("samples/type_a_large_50k.xlsx", 50000)

    print("\n📁 모든 대용량 파일 생성 완료!")
