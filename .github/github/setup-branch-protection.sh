#!/bin/bash

if [ -z "$GITHUB_TOKEN" ]; then
  echo "Error: GITHUB_TOKEN environment variable is not set"
  echo "Usage: export GITHUB_TOKEN='your_token' && ./setup-branch-protection.sh [branch]"
  exit 1
fi

REMOTE_URL=$(git config --get remote.origin.url)
OWNER=$(echo $REMOTE_URL | sed -n 's#.*github.com[:/]\([^/]*\)/.*#\1#p')
REPO=$(echo $REMOTE_URL | sed -n 's#.*github.com[:/].*/\(.*\)\.git#\1#p')
REPO=${REPO%.git}
BRANCH=${1:-main}
TOKEN=$GITHUB_TOKEN

echo "Repository: $OWNER/$REPO"
echo "Branch: $BRANCH"
echo ""

curl -X PUT \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/$OWNER/$REPO/branches/$BRANCH/protection" \
  -d '{
    "required_status_checks": {
      "strict": true,
      "contexts": ["build"]
    },
    "enforce_admins": false,
    "required_pull_request_reviews": null,
    "restrictions": null
  }'

echo ""
echo "Branch protection for $BRANCH has been set up!"
echo "CI check 'build' is now required before merging."
