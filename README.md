# wp-integrity

A command line tool that examines a WordPress installation on disk, read-only, and points out signs of
tampering: core files that were modified or that should not be there, and PHP where there should be none.
It is not an antivirus. It reports indications for a person to review and never deletes or changes anything.

WP-CLI already verifies core files with `wp core verify-checksums`. What this tool adds is that it runs
without PHP and without WP-CLI (for example in a container with the folder mounted read-only), it also looks
at the uploads directory and at stray PHP files in the root, and it writes its report as JSON and as HTML.

## Running it

Build and run the JAR (JDK 21 and Maven 3.9):

    mvn -B verify
    java -jar target/wp-integrity.jar /path/to/wordpress

With Docker, mounting the site read-only:

    docker build -t wp-integrity .
    docker run --rm -v /path/to/wordpress:/site:ro wp-integrity /site

The container runs as `nobody`, so files with restrictive permissions may not be readable. Those are reported
as `SKIPPED`. To read them, run the container as the owner of the files:

    docker run --rm --user "$(id -u):$(id -g)" -v /path/to/wordpress:/site:ro wp-integrity /site

To write a report file from the container, mount a writable folder outside the site:

    docker run --rm --user "$(id -u):$(id -g)" -v /path/to/wordpress:/site:ro -v /path/to/reports:/out \
      wp-integrity /site --format html --output /out/report.html

Options:

| Option | Effect |
| --- | --- |
| `--format text\|json\|html` | Report format, default `text` |
| `--output <file>` | Write the report to a file. Required for `html`. Refused if the file would be inside the scanned folder |
| `--checksums <file.json>` | Use a saved api.wordpress.org response instead of calling the API (offline mode) |
| `--wp-version <v>`, `--locale <l>` | Set the version and language when `wp-includes/version.php` cannot be read |
| `--fail-on none\|warning\|error` | Lowest severity that makes the exit code 1, default `error` |
| `--scan-plugins` | Also search plugins and themes for suspicious patterns (off by default: many false positives) |

The version and language come from `wp-includes/version.php` (`$wp_version`, `$wp_local_package`, `en_US` when
absent). The official checksums are fetched from `https://api.wordpress.org/core/checksums/1.0/`. That address is
fixed in the code and there is no option to change it.

## Checks

| Id | Severity | Meaning |
| --- | --- | --- |
| `CORE_MODIFIED` | ERROR | Core file whose hash differs from the official one |
| `PHP_IN_UPLOADS` | ERROR | `.php`, `.phtml`, `.phar`, `.pht` or `.php[0-9]` file inside `wp-content/uploads` |
| `CORE_MISSING` | WARNING | Core file that is missing |
| `UNKNOWN_IN_CORE_DIR` | WARNING | File in `wp-admin/` or `wp-includes/` that is not in the official checksums |
| `EXTRA_PHP_IN_ROOT` | WARNING | `.php` file in the root that is not part of core (except `wp-config.php`) |
| `WORLD_WRITABLE` | WARNING | PHP file writable by any user (POSIX systems only) |
| `SUSPICIOUS_PATTERN` | INFO | Code that often indicates obfuscation or remote execution; for review only |
| `SKIPPED` | INFO | File that was unreadable or larger than 2 MiB, ignored by the pattern search |

The contents of `wp-content/` are not compared with core, since official core does not include it. Uploads
have their own check.

Suspicious patterns are searched line by line, and only in PHP files in uploads, unknown files in `wp-admin/` and
`wp-includes/`, extra PHP in the root and, with `--scan-plugins`, plugins and themes. The patterns are:

- `eval` applied to `base64_decode`, `gzinflate`, `gzuncompress` or `str_rot13`
- `system`, `exec`, `shell_exec`, `passthru`, `popen` or `proc_open` receiving `$_GET`, `$_POST`, `$_REQUEST` or `$_COOKIE`
- `assert` receiving request input
- `preg_replace` with the `/e` modifier
- a literal of 256 or more base64 characters passed to `base64_decode`

Each finding carries the file, the line number and a snippet cut at 120 characters.

Compared with `wp core verify-checksums`:

| | wp-integrity | WP-CLI |
| --- | --- | --- |
| Needs PHP and WP-CLI | no | yes |
| Modified and missing core files | yes | yes |
| Unknown files in `wp-admin/` and `wp-includes/` | yes | yes |
| Uploads directory | yes | no |
| Stray PHP in the root | yes | no |
| JSON and HTML reports | yes | no |

## Output and exit codes

`text` lists findings by severity and then path, with a summary line at the end. `json` has the tool version, the
WordPress version, counts and the list of findings. `html` is a single self-contained file with inline styles,
no scripts and nothing loaded from elsewhere; every file name and snippet is escaped. Paths always use `/` and
are relative to the scanned folder.

| Code | Meaning |
| --- | --- |
| 0 | Nothing at the requested level |
| 1 | Findings at the requested level or above |
| 2 | Usage or runtime error |

If the API cannot be reached, or does not know the version, the tool exits with 2 and a message. A failed lookup
never looks like a clean result.

The tool never opens a file of the scanned folder for writing and does not follow symbolic links. The hash is
computed by streaming the file.

## What it does not do

- It is not an antivirus and has no malware signatures. The patterns are heuristics and produce false positives.
- It does not delete, quarantine or change anything.
- It does not inspect the database.
- It does not verify plugins and themes against the WordPress.org repository.
- It depends on api.wordpress.org being reachable, unless you pass `--checksums`.
- The API only offers MD5 checksums, so that is what is compared.
- Patterns are matched one line at a time; code split across lines is not caught.
- Files in uploads with a double extension such as `shell.php.jpg` are not flagged.
- It runs sequentially.

## About

wp-integrity is a small personal project by Daniel Angelone.
