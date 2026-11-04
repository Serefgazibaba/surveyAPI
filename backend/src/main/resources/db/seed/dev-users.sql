-- Dev ortamı seed kullanıcıları. YALNIZCA dev profilinde çalışır (application-dev.yml → spring.sql.init).
-- Tekrar çalıştırılabilir: aynı e-posta varsa satır atlanır.
--
-- password_hash: BCrypt (cost 10). Salt hash metninin içindedir: $2a$10$<22 karakter salt><31 karakter hash>
-- Her satırın salt'ı farklıdır; aynı şifre farklı hash üretir.
--
-- | E-posta                 | Rol   | Şifre     |
-- |-------------------------|-------|-----------|
-- | ayse.yilmaz@definex.com | ADMIN | Admin123! |
-- | mehmet.demir@definex.com| ADMIN | Admin123! |
-- | zeynep.kaya@definex.com | USER  | User123!  |
-- | can.ozturk@definex.com  | USER  | User123!  |
-- | elif.sahin@definex.com  | USER  | User123!  |

INSERT INTO users (id, full_name, email, password_hash, role, status, created_at, updated_at)
VALUES
    ('0a1b2c3d-0000-4000-8000-000000000001', 'Ayşe Yılmaz',   'ayse.yilmaz@definex.com',
     '$2a$10$cUtNCCpp44Xfx2ZNbP5I9OfzvsPWh9vV.5it7Fy5cADKel7hgkmn.', 'ADMIN', 'ACTIVE', now(), now()),
    ('0a1b2c3d-0000-4000-8000-000000000002', 'Mehmet Demir',  'mehmet.demir@definex.com',
     '$2a$10$Tytlb/dN2YvDrhkKr2VqCeYagQsim/h7COmh5/aTA6QUr34grD1kS', 'ADMIN', 'ACTIVE', now(), now()),
    ('0a1b2c3d-0000-4000-8000-000000000101', 'Zeynep Kaya',   'zeynep.kaya@definex.com',
     '$2a$10$w0Ifqzu8ExSrwsGw//p73u.BvI3.hQ5BLhAhZ6vHkBlVYwkCwmJlu', 'USER', 'ACTIVE', now(), now()),
    ('0a1b2c3d-0000-4000-8000-000000000102', 'Can Öztürk',    'can.ozturk@definex.com',
     '$2a$10$FjWFKn78RNTH9Ic8RhBh/Om6I16d/bhJ8lo2MZA40MkdQfx/eQby2', 'USER', 'ACTIVE', now(), now()),
    ('0a1b2c3d-0000-4000-8000-000000000103', 'Elif Şahin',    'elif.sahin@definex.com',
     '$2a$10$/eO1uDXXAIodRXrbXxtKIek6tHsEuEee/eoVktOygvAZ0YVMmZrAm', 'USER', 'ACTIVE', now(), now())
ON CONFLICT (email) DO NOTHING;
