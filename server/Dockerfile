FROM python:3.12-slim

WORKDIR /app
RUN pip install --no-cache-dir "fastapi>=0.115" "uvicorn[standard]>=0.30" "pillow>=10"
COPY main.py dashboard.py ./
COPY fonts ./fonts

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
