#!/bin/bash
lgm() { date "+%c%t$1"; }
[[ "$TRAVIS_PULL_REQUEST" != false || "$TRAVIS_BRANCH" != master ]] \
	&& exit || lgm "Generating Javadocs..."
myid=${TRAVIS_REPO_SLUG%/*}
myproj=${TRAVIS_REPO_SLUG##*/}
jdb=gh-pages
websrc=$TRAVIS_BUILD_DIR/target/site/apidocs
webtgt=$jdb
mvn javadoc:javadoc
cd $HOME
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"
lgm "Cloning..."; 
	git clone -q -b $jdb https://${gh_token}@github.com/$myid/$myproj $webtgt > /dev/null
lgm "Copying..."; 
	cd $webtgt; rm -rf apidocs && cp -Rf $websrc .
lgm "Adding..."; 
	git add -f . > /dev/null
lgm "Committing..."; 
	git commit -m "Published Javadoc: build $TRAVIS_BUILD_NUMBER" > /dev/null
lgm "Pushing..."; 
	git push -fq origin $jdb > /dev/null
lgm "Done."
