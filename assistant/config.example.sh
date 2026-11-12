# Copy these settings into your TERMINAL; do not put real credentials in this file.
# The Python server does not automatically load .env files.
export ASSISTANT_MODE=live
export INVENTORY_MODE=live
export OPENAI_BASE_URL=https://models.sjtu.edu.cn/api/v1
export OPENAI_MODEL=deepseek-chat
# Set OPENAI_API_KEY privately in the terminal before starting the service.
export BOOKSTORE_API_BASE=http://127.0.0.1:8080/api/v1
export ASSISTANT_PORT=8001
# Optional. If omitted, server.py uses its own actual port for the mock API.
# export COMPETITOR_API_URL=http://127.0.0.1:8001/mock/competitor-price
# For an offline demo instead, explicitly use BOTH settings:
# export ASSISTANT_MODE=demo
# export INVENTORY_MODE=demo
