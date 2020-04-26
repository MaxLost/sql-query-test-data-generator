CREATE TABLE t1 (id INTEGER GENERATED BY DEFAULT AS IDENTITY, "Product" VARCHAR(50))

CREATE TABLE t2 (t1_id INT, "Type" INT, "Price" FLOAT) 

INSERT INTO t1 ("Product") VALUES ('A'), ('B'), ('C')

INSERT INTO t2 VALUES (0, 1, 5), (0, 2, 3), (0, 3, 3), (1, 1, 10), (1, 2, 8), (2, 1, 7)

SELECT "Product", AVG("Price")
FROM t1
JOIN t2 ON t1.id = t2.t1_id
GROUP BY "Product"
HAVING AVG("Price") > 6

DROP TABLE t1

DROP TABLE t2