FROM python:3.12-slim

WORKDIR /app
ENV PYTHONUNBUFFERED=1

COPY validator/requirements.txt validator/requirements.txt
COPY validator/requirements-model.txt validator/requirements-model.txt
RUN pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir -r validator/requirements-model.txt

COPY validator validator
RUN mkdir -p validator/normalized

EXPOSE 8001
CMD ["python", "-m", "uvicorn", "validator.app:app", "--host", "0.0.0.0", "--port", "8001"]
