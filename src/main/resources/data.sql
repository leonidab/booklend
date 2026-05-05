-- =============================================================
-- Seed data — state as of 2026-05-05
-- =============================================================

-- ── BOOKS ────────────────────────────────────────────────────
-- b1  Clean Code           UNAVAILABLE  (on loan to Alice)
-- b2  Pragmatic Programmer UNAVAILABLE  (on loan to Bob, overdue)
-- b3  Domain-Driven Design AVAILABLE
-- b4  Design Patterns      AVAILABLE
-- b5  Refactoring          AVAILABLE
-- b6  Working Effectively  UNAVAILABLE  (on loan to Bob, active)
INSERT INTO books (id, isbn, title, author, available) VALUES
  ('a1b2c3d4-0000-0000-0000-000000000001', '9780132350884', 'Clean Code',                               'Robert C. Martin',       false),
  ('a1b2c3d4-0000-0000-0000-000000000002', '9780201633610', 'The Pragmatic Programmer',                 'Hunt & Thomas',          false),
  ('a1b2c3d4-0000-0000-0000-000000000003', '9780321125217', 'Domain-Driven Design',                    'Eric Evans',             true),
  ('a1b2c3d4-0000-0000-0000-000000000004', '9780201309806', 'Design Patterns',                         'Gang of Four',           true),
  ('a1b2c3d4-0000-0000-0000-000000000005', '9780201485677', 'Refactoring',                             'Martin Fowler',          true),
  ('a1b2c3d4-0000-0000-0000-000000000006', '9780131177055', 'Working Effectively with Legacy Code',    'Michael Feathers',       false);

-- ── MEMBERS ──────────────────────────────────────────────────
-- m1  Alice  ACTIVE      1 active loan,  0 late returns
-- m2  Bob    ACTIVE      2 active loans, 1 late return  (has overdue loan)
-- m3  Carol  RESTRICTED  0 active loans, 3 late returns (auto-restricted)
-- m4  David  ACTIVE      0 active loans, 0 late returns (has a reservation queued)
INSERT INTO members (id, name, email, status, active_loans_count, late_return_count) VALUES
  ('d1e2f3a4-0000-0000-0000-000000000001', 'Alice Johnson', 'alice@booklend.dev', 'ACTIVE',      1, 0),
  ('d1e2f3a4-0000-0000-0000-000000000002', 'Bob Smith',     'bob@booklend.dev',   'ACTIVE',      2, 1),
  ('d1e2f3a4-0000-0000-0000-000000000003', 'Carol White',   'carol@booklend.dev', 'RESTRICTED',  0, 3),
  ('d1e2f3a4-0000-0000-0000-000000000004', 'David Brown',   'david@booklend.dev', 'ACTIVE',      0, 0);

-- ── LOANS ────────────────────────────────────────────────────
-- Active loans
--   l1  Alice  → Clean Code           borrowed 2026-04-28  due 2026-05-12  (7 days left)
--   l2  Bob    → Pragmatic Programmer  borrowed 2026-04-10  due 2026-04-24  OVERDUE by 11 days
--   l3  Bob    → Legacy Code           borrowed 2026-04-28  due 2026-05-12  (7 days left)
-- Returned loans (history)
--   l4  Carol  → Refactoring           returned 10 days late → lateReturnCount +1
--   l5  Carol  → Design Patterns       returned  9 days late → lateReturnCount +1
--   l6  Carol  → Domain-Driven Design  returned 10 days late → lateReturnCount +1, triggers RESTRICTED
--   l7  Bob    → Refactoring           returned  5 days late → lateReturnCount +1
INSERT INTO loans (id, member_id, book_id, borrowed_at, due_date, returned_at, status) VALUES
  -- active
  ('f1a2b3c4-0000-0000-0000-000000000001',
      'd1e2f3a4-0000-0000-0000-000000000001',
      'a1b2c3d4-0000-0000-0000-000000000001',
      TIMESTAMP WITH TIME ZONE '2026-04-28 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-05-12 09:00:00+00:00',
      NULL, 'ACTIVE'),

  ('f1a2b3c4-0000-0000-0000-000000000002',
      'd1e2f3a4-0000-0000-0000-000000000002',
      'a1b2c3d4-0000-0000-0000-000000000002',
      TIMESTAMP WITH TIME ZONE '2026-04-10 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-04-24 09:00:00+00:00',
      NULL, 'ACTIVE'),

  ('f1a2b3c4-0000-0000-0000-000000000003',
      'd1e2f3a4-0000-0000-0000-000000000002',
      'a1b2c3d4-0000-0000-0000-000000000006',
      TIMESTAMP WITH TIME ZONE '2026-04-28 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-05-12 09:00:00+00:00',
      NULL, 'ACTIVE'),

  -- returned (Carol — 3 late returns)
  ('f1a2b3c4-0000-0000-0000-000000000004',
      'd1e2f3a4-0000-0000-0000-000000000003',
      'a1b2c3d4-0000-0000-0000-000000000005',
      TIMESTAMP WITH TIME ZONE '2026-02-01 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-02-15 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-02-25 09:00:00+00:00',
      'RETURNED'),

  ('f1a2b3c4-0000-0000-0000-000000000005',
      'd1e2f3a4-0000-0000-0000-000000000003',
      'a1b2c3d4-0000-0000-0000-000000000004',
      TIMESTAMP WITH TIME ZONE '2026-02-15 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-03-01 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-03-10 09:00:00+00:00',
      'RETURNED'),

  ('f1a2b3c4-0000-0000-0000-000000000006',
      'd1e2f3a4-0000-0000-0000-000000000003',
      'a1b2c3d4-0000-0000-0000-000000000003',
      TIMESTAMP WITH TIME ZONE '2026-03-01 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-03-15 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-03-25 09:00:00+00:00',
      'RETURNED'),

  -- returned (Bob — 1 late return)
  ('f1a2b3c4-0000-0000-0000-000000000007',
      'd1e2f3a4-0000-0000-0000-000000000002',
      'a1b2c3d4-0000-0000-0000-000000000005',
      TIMESTAMP WITH TIME ZONE '2026-03-01 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-03-15 09:00:00+00:00',
      TIMESTAMP WITH TIME ZONE '2026-03-20 09:00:00+00:00',
      'RETURNED');

-- ── RESERVATIONS ─────────────────────────────────────────────
-- r1  David → Clean Code           (loan l1 by Alice still active)
-- r2  Carol → Pragmatic Programmer (loan l2 by Bob still active, overdue)
INSERT INTO reservations (id, book_id, member_id, requested_at) VALUES
  ('e1d2c3b4-0000-0000-0000-000000000001',
      'a1b2c3d4-0000-0000-0000-000000000001',
      'd1e2f3a4-0000-0000-0000-000000000004',
      TIMESTAMP WITH TIME ZONE '2026-05-01 14:00:00+00:00'),

  ('e1d2c3b4-0000-0000-0000-000000000002',
      'a1b2c3d4-0000-0000-0000-000000000002',
      'd1e2f3a4-0000-0000-0000-000000000003',
      TIMESTAMP WITH TIME ZONE '2026-05-03 11:00:00+00:00');
