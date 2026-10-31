-- Provider identities are owned by the central Webtools Identity Server now.
-- Keep user_entity as the Kanban-local profile keyed by the central identity UUID.
DROP TABLE IF EXISTS user_provider;
