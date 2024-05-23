SELECT problems.fullname, testcases.inputdata, testcases.expectedresult, testcases.isValid
FROM testcases
INNER JOIN problems ON testcases.problems_id = problems.id
ORDER BY problems.fullname