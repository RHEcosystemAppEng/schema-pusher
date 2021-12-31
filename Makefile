default: help

ifndef VERSION
$(error Use VERSION to set the version of the image)
endif

IMAGE_NAME = quay.io/tomerfi/schema-pusher
CURRENT_DATE = $(strip $(shell date -u +"%Y-%m-%dT%H:%M:%SZ"))
FULL_IMAGE_NAME = $(strip $(IMAGE_NAME):$(VERSION))

PLATFORMS = linux/amd64

.PHONY: build push

help:
    $(info please use targets build or push)

build:
	docker buildx build \
	--build-arg BUILD_DATE=$(CURRENT_DATE) \
	--build-arg VERSION=$(VERSION) \
	--output type=docker \
	--tag $(FULL_IMAGE_NAME) .

push:
	docker buildx build \
	--build-arg BUILD_DATE=$(CURRENT_DATE) \
	--build-arg VERSION=$(VERSION) \
	--output type=registry \
	--tag $(FULL_IMAGE_NAME) \
	--tag $(IMAGE_NAME):latest .
