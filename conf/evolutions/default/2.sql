# --- !Ups
-- ちなみにパスワードすべて hoge の MD5 です。
INSERT INTO USER (ID, NAME, PASSWORD) VALUES
  (1, 'user1', 'ea703e7aa1efda0064eaa507d9e8ab7e'),
  (2, 'user2', 'ea703e7aa1efda0064eaa507d9e8ab7e'),
  (3, 'user3', 'ea703e7aa1efda0064eaa507d9e8ab7e');

INSERT INTO PRODUCT (ID, NAME, PRICE) VALUES
  (1, 'MacBookPro 13インチ', 148800),
  (2, 'MacBookPro 13インチ Touch Bar', 178800),
  (3, 'MacBookPro 15インチ Touch Bar', 198800);

# --- !Downs
DELETE FROM PRODUCT WHERE 1;
DELETE FROM USER WHERE 1;
