#!/bin/bash
lgm() { date "+%c%t$1"; }
[[ "$TRAVIS_PULL_REQUEST" != false || "$TRAVIS_BRANCH" != master ]] \
	&& exit || lgm "Generating Javadocs..."
myid=${TRAVIS_REPO_SLUG%/*}
myproj=${TRAVIS_REPO_SLUG##*/}
jdb=gh-pages
websrc=$TRAVIS_BUILD_DIR/target/site/apidocs
webtgt=$(mktemp -d)
[ -z "$webtgt" ] && lgm "Error creating tmp dir, bailing." && exit 1
mvn javadoc:javadoc
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"
lgm Cloning... 
	git clone -qb $jdb https://${gh_token}@github.com/$myid/$myproj "$webtgt"
lgm Refreshing...
	rsync -qr --delete "$websrc" "$webtgt"
lgm Adding...
	cd "$webtgt" && git add -Af > /dev/null
lgm Committing...
	git commit -qm "Published Javadoc: build $TRAVIS_BUILD_NUMBER"
lgm Pushing... 
	git push -fq origin $jdb
lgm Done.