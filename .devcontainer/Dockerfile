FROM mcr.microsoft.com/devcontainers/java:25

RUN apt-get update && apt-get install -y --no-install-recommends \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libgl1 \
    libglx-mesa0 \
    libgl1-mesa-dri \
    xauth \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -g 105 input || true && \
    usermod -aG input vscode