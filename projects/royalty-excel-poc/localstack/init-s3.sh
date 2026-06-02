#!/bin/bash
set -e

echo "=== Creating S3 bucket ==="

awslocal s3 mb s3://royalty-excel-uploads

echo "=== S3 bucket created: royalty-excel-uploads ==="
