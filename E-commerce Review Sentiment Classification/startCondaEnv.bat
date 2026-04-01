@echo off
echo Activating sentiment env...

:: 激活 conda 环境
call conda activate sentiment

:: 进入项目 src 目录
cd /d "e:\graduation project\E-commerce Review Sentiment Classification\src"

echo Python version:
python --version

echo Start app.py
python app.py

:: 保持终端打开
cmd /k