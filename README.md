# sactilgis

## Purpose

The main purpose of sactilgis is to enable a user to create a Git repository for a project that’s stored in a Subversion server, potentially as one of many projects. It also aims to handle situations that arise from imperfect use of Subversion, such as non-standard repository layouts, branches without history, manual, unrecorded merges, modified tags, and whatever else a company might throw at a Subversion repository.

However, due to the large number of things you can do to a Subversion repository, only the really menial tasks can be automated; sactilgis needs your help for some high-level tasks, such as which branches do exist and where (and when!) are they, where are branches merged, which version has actually been tagged. With that information sactilgis should be able to create a reasonable Git representation of your project, straight from Subversion.

## Building

sactilgis comes with its own Maven wrapper, so building it should be as easy as:

    # ./mvwn clean package

After that, a nice, big JAR file will have been dropped in `app/target/`.

    # java -jar app/target/app-1-SNAPSHOT-jar-with-dependencies.jar my-configuration.xml

And already your conversion should be underway!

## Configuration

The configuration is done using an XML file. Under the top level `configuration` tag, there are three more sections that are used to control all of sactilgis’s behaviour: `general`, `committers`, and `branches`.

```xml
<?xml version="1.0" encoding="utf-8"?>
<configuration>
	<general>
		…
	</general>
	<committers>
		…
	</committers>
	<branches>
		…
	</branches>
</configuration>
```

### The `general` Section

This section contains certain top-level configuration settings.

subversion-url
: The URL of the Subversion repository. Supports at least `svn://` and `file://` repository URLs; may support `http://` and `https://` as well (has never been tested).

target-directory
: The directory in which to store the resulting Git repository. This directory will be *removed and re-created* before the conversion starts!

committer
: The name and email address of the person doing the conversion. This name and email address will be used for the committer data, and for tags. Format is the same as for the [committers](#the-committers-section); however, the `id` is optional. If this element is omitted, the original commit author is used as committer.

use-commit-date-from-entry
: If `true`, the author date of the Subversion commits are used as commit date for the Git commits. The author date of the Git commit is always taken from the Subversion commit date.

ignore-global-gitignore-file
: If `true`, a globally configured `.gitignore` file (configured by `core.excludesFile` using `git config`) will be ignored when commits are created. Setting this to `false` may lead to repositories with all files defined in your `.gitignore` file missing which may or may not be the intended consequence. As I currently consider the use of sactilgis to be a matter of keeping history intact as much as possible, I would recommend setting this to `true`.

sign-commits
: If `true`, all Git commits and tags will be signed. The necessary configuration (e.g. the user’s signing key) have to be configured outside sactilgis.

### The `committers` Section

Author data stored in Subversion boils down to a single username; no full name or email address in sight anywhere. As such, the usernames need to be translated into full author details, and that is the purpose of this section.

Each committer gets their own `committer` element:

```xml
<committer>
	<id>dr</id>
	<name>David Roden</name>
	<email>bombe@pterodactylus.net</email>
</commiter>
```

Repeat this tag as often as necessary to include all authors that are a part of your project’s Subversion history.

id
: This is the username of the author of the Subversion commit. It will be used for the mapping.

name
: The full name of the author that will be stored in the Git commit.

email
: The email address of the author stored in the Git commit.

### The `branches` section

This is finally where the meat of the configuration file lives. Here all the branches (including the main branch, often refered to as “trunk” in Subversion-land) that are a part of your project need to be defined, because it’s nigh impossible to generate this data from the information in the Subversion repository; mostly, because Subversion doesn’t know what a branch even _is_, and developers are incredibly skilled at working around restrictions in software and gaps in their knowledge while still appearing to know it all. 😄

Each branch has a number of features; a name, a list of revision-dependent paths for locating commits belonging to it, merge commits, tags, and fixes.

```xml
<branch>
	<name>main</name>
	<revision-paths>
		<revision-path>
			<revision>5</revision>
			<path>/trunk</path>
		</revision-path>
		<revision-path>
			<revision>841</revision>
			<path>/project1/trunk</path>
		</revision-path>
	</revision-paths>
	<merges>
		<merge>
			<revision>267</revision>
			<branch>new-feature</branch>
		</merge>
	</merges>
	<tags>
		<tag>
			<revision>318</revision>
			<name>0.4</name>
			<message-revision>325</message-revision>
		</tag>
	</tags>
	<fixes>
		<fix>
			<revision>13</revision>
			<message>Tëstïng för ümläutß</message>
		</fix>
	</fixes>
</branch>
```

The first branch in your `branches` section needs to be the one in your repository that the first commit you will be processing belongs to. For most repositories, this should be the main branch.

At the moment, the requirement that the first branch has to be named “main” is hard-coded.

This sections needs to be repeated for every branch you want to transfer into a Git repository.

#### The `revision-paths` section

A revision-path is a point at which a branch either comes into existence for the first time, or changes location in the repository.

Each branch only exists at a single location at a specific revision; in the example above, if a commit at revision 845 (i.e. four commits *after* its location has changed) has been done on `/trunk`, it will *not* be a part of that branch anymore!

revision
: The revision at which the branch changes location. The commit at this revision already lives at the new location.

path
: The location in the repository that your branch lives on. It will continue to live at this location until the next revision-path entry (or HEAD, whichever comes first).

#### The `merges` section

A merge defines a point where two branches are joined. This information cannot reliably be pulled from Subversion’s `svn:mergeinfo` property, because the property can be set on nested branches somewhere, or the mergeinfo property only contains a handful of commits from the merged branch. Git cannot handle selective merges like that, so here *you* have to decide if a commit in Subversion should be treated like a merge in Git, i.e. unifying two lines of development.

revision
: The revision at which the branch should be recorded.

branch
: The branch that should be merged into this branch. Obviously, this branch needs to be defined in this configuration file as well.

#### The `tags` section

Tags are markers for certain commits, most often used for released versions. But, just like Subversion doesn’t know what a branch is, it also doesn’t know what a tag is; Subversion just sees directories in a sea of directories. So once again, your help is required!

revision
: The revision of this branch that should be tagged. The actual revision the tag will be made of may actually be lower than this; if the Subversion commit (e.g. on your `/tags` branch) refers to revision 10 but the last commit on this branch was actually 7, revision 7 will be tagged.

name
: The name of the tag. Tag names need to be unique in a repository.

message-revision
: The revision that the message of the Git tag should be taken from.

#### The `fixes` section

If you live a country that does not have US-ASCII-compatible national language, you may encounter problems with those pesky accented or otherwise multi-byted characters. This section allows you to replace messages that have not been converted correctly.

revision
: The revision to fix.

message
: The new message for this revision.

## TODO

There are several things that I still want to implement:

* Handling of empty commits: often, when a new branch is created from an existing branch, the initial commit only contains a copy within Subversion, i.e. no changes are actually being done. Empty commits in Git are possible (and sactilgis already creates them) but they are kind of ugly and (mostly) pointless. I want to evaluate if simply skipping these commits is acceptable; it does also throw away the commit message which may or may not be important.
* Commit rewriting: sometimes, commits are a real mess. Initially this project was aimed at creating reasonable Git versions of Subversion project repositories, but why stop there? When commits can be surgically altered during conversion, it might just be possible to craft the perfect Git repository, even if not 100% historically accurate. It might come in handy, you never know!
