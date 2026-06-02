#!/bin/bash
set -e

echo "=== LocalStack Init Start ==="

# S3 버킷 생성
awslocal s3 mb s3://file-upload-bucket
echo "S3 bucket created: file-upload-bucket"

# SQS 큐 생성
awslocal sqs create-queue --queue-name file-upload-queue
echo "SQS queue created: file-upload-queue"

# SQS 큐 ARN 조회
QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/file-upload-queue \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

echo "Queue ARN: $QUEUE_ARN"

# SQS 큐에 S3 이벤트 알림 권한 부여
awslocal sqs set-queue-attributes \
    --queue-url http://localhost:4566/000000000000/file-upload-queue \
    --attributes '{
        "Policy": "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"s3.amazonaws.com\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"'$QUEUE_ARN'\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"arn:aws:s3:::file-upload-bucket\"}}}]}"
    }'

# S3 버킷에 이벤트 알림 설정 (SQS 연결)
awslocal s3api put-bucket-notification-configuration \
    --bucket file-upload-bucket \
    --notification-configuration '{
        "QueueConfigurations": [{
            "QueueArn": "'$QUEUE_ARN'",
            "Events": ["s3:ObjectCreated:*"]
        }]
    }'

echo "=== S3 -> SQS event notification configured ==="
echo "=== LocalStack Init Complete ==="
