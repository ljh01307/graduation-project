import random
import csv
from datetime import datetime, timedelta

PRODUCT_ID = 28

START_LINE = 8300
END_LINE = 9000

def random_time(start_str, end_str):
    start = datetime.strptime(start_str, '%Y-%m-%d')
    end = datetime.strptime(end_str, '%Y-%m-%d')
    delta = end - start
    random_seconds = random.randint(0, int(delta.total_seconds()))
    return (start + timedelta(seconds=random_seconds)).strftime('%Y-%m-%d %H:%M:%S')

def convert_to_sql(input_file, output_file, start_line, end_line):
    reviews = []
    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader)
        for i, row in enumerate(reader, start=1):
            if start_line <= i <= end_line:
                if len(row) >= 3:
                    reviews.append(row[2])

    if not reviews:
        print(f"没有找到第 {start_line} 到 {end_line} 行的评论")
        return

    sql_lines = [f"-- 生成了 {len(reviews)} 条评论 SQL (第 {start_line} 到 {end_line} 行)\n"]
    sql_lines.append("INSERT INTO review (product_id, content, sentiment_label, confidence, is_analyzed, upload_time, analyze_time) VALUES\n")

    values = []
    for content in reviews:
        content_escaped = content.replace("'", "''")
        upload_time = random_time('2026-03-01', '2026-05-06')
        values.append(f"({PRODUCT_ID}, '{content_escaped}', NULL, NULL, 0, '{upload_time}', NULL)")

    sql_lines.append(',\n'.join(values) + ';')

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sql_lines))

    print(f"已生成 {len(reviews)} 条SQL，输出到 {output_file}")

if __name__ == '__main__':
    convert_to_sql('online_shopping_10_cats.csv', 'output.sql', START_LINE, END_LINE)
