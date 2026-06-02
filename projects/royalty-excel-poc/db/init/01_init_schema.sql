-- 협회별 엑셀 매핑 정보
CREATE TABLE IF NOT EXISTS excel_type_mappings (
    id SERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    header_row_start INT NOT NULL,
    target_table VARCHAR(100),
    column_mappings JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 엑셀 업로드 이력
CREATE TABLE IF NOT EXISTS excel_uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500),
    s3_bucket VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PROCESSING',
    total_rows INT,
    processed_rows INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 저작권료 데이터 (행별 JSONB 저장)
CREATE TABLE IF NOT EXISTS royalty_records (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID NOT NULL REFERENCES excel_uploads(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    original_data JSONB NOT NULL,
    matched_data JSONB NOT NULL,
    header_mapping JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_royalty_records_upload_id ON royalty_records(upload_id);
CREATE INDEX IF NOT EXISTS idx_excel_uploads_type ON excel_uploads(type);
CREATE INDEX IF NOT EXISTS idx_excel_uploads_status ON excel_uploads(status);
CREATE INDEX IF NOT EXISTS idx_royalty_records_matched_data ON royalty_records USING GIN (matched_data);

-- 4가지 협회 타입 초기 데이터
INSERT INTO excel_type_mappings (type, type_name, header_row_start, column_mappings) VALUES
('A', '음반협회', 1, '{
    "저작권료": "royalty_fee",
    "저작자": "copyright_holder",
    "작품명": "work_title",
    "사용일": "usage_date",
    "판매량": "sales_count"
}'),
('B', '방송협회', 3, '{
    "지급액": "payment_amount",
    "권리자": "rights_holder",
    "프로그램명": "program_name",
    "방송일": "broadcast_date",
    "방송시간": "broadcast_duration"
}'),
('C', '영화협회', 2, '{
    "상영료": "screening_fee",
    "제작사": "production_company",
    "영화제목": "movie_title",
    "개봉일": "release_date",
    "관객수": "audience_count"
}'),
('D', '공연협회', 1, '{
    "공연료": "performance_fee",
    "공연자": "performer",
    "공연명": "performance_title",
    "공연일": "performance_date",
    "티켓판매": "ticket_sales"
}')
ON CONFLICT (type) DO NOTHING;
