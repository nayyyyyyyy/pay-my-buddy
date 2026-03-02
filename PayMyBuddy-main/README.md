# PayMyBuddy

Application prototype permettant à des utilisateurs de transférer de l’argent entre amis et de gérer leurs connexions.

MPD textuel : 

USERS (
    id PK, 
    username UNIQUE, 
    email UNIQUE, 
    password, 
    created_at, 
    updated_at, 
    is_active
)

TRANSACTIONS (
  id PK,
  sender_id FK → USERS(id),
  receiver_id FK → USERS(id),
  description,
  amount,
  created_at
)

USER_CONNECTION (
  user_id PK FK → USERS(id),
  connection_id PK FK → USERS(id),
  created_at
)