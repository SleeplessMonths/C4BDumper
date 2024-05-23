SELECT
    source.id,
    source.submission,
    source.sourceCode,
    source.author,
    source.sent,
    languages.name AS language,
    problems.fullname,
    realfaultslocations.subaccepted,
    realfaultslocations.faultlocations,
    realfaultslocations.countfaults
FROM
    source
INNER JOIN
    realfaultslocations ON source.submission = realfaultslocations.subwrong
INNER JOIN
    problems ON source.problems_id = problems.id
INNER JOIN
    languages ON source.languages_id = languages.id
INNER JOIN
    verdicts ON source.verdicts_id = verdicts.id
LEFT JOIN
    testcases ON testcases.problems_id = source.problems_id
            AND (
                testcases.expectedresult LIKE "%..."
                OR testcases.inputdata LIKE "%..."
            )
WHERE
    source.isduplicated = 0
    AND realfaultslocations.countfaults = 1
    AND languages.name LIKE "Java _"
    AND verdicts.name LIKE 'Wrong answer on test'
    AND testcases.inputdata IS NULL
    AND testcases.expectedresult IS NULL
    AND NOT EXISTS (
        SELECT
            1
        FROM
            source AS s2
        INNER JOIN
            languages AS l2 ON l2.id = s2.languages_id
        WHERE
            s2.submission = realfaultslocations.subaccepted
            AND l2.name NOT LIKE "Java _"
    )
ORDER BY
    source.sent
DESC;