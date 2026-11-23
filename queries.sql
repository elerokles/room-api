-- Q1
SELECT * FROM room
WHERE capacity >= 6
ORDER BY capacity DESC;
-- Q2
SELECT * FROM reservation
WHERE reserved_by = 'Mina';

-- Q3
SELECT r.*, rm.name AS room_name
FROM reservation r
JOIN room rm ON rm.id = r.room_id;

-- Q4
SELECT r.*
FROM reservation r
JOIN room rm ON rm.id = r.room_id
WHERE rm.name = 'Seminar A'
  AND CAST(r.start_time AS DATE) = '2026-10-06';

-- Q5
SELECT rm.name, COUNT(*) AS reservation_count
FROM room rm
JOIN reservation r ON r.room_id = rm.id
GROUP BY rm.id, rm.name;

-- Q6
SELECT rm.name, COUNT(r.id) AS reservation_count
FROM room rm
LEFT JOIN reservation r ON r.room_id = rm.id
GROUP BY rm.id, rm.name;

-- Q7
SELECT rm.*
FROM room rm
LEFT JOIN reservation r ON r.room_id = rm.id
WHERE r.id IS NULL;

-- Q8
SELECT rm.name, COUNT(r.id) AS reservation_count
FROM room rm
JOIN reservation r ON r.room_id = rm.id
GROUP BY rm.id, rm.name
HAVING COUNT(r.id) > 2;
