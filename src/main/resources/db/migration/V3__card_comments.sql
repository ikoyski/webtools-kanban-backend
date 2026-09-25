-- Migration for Card Comments
CREATE TABLE card_comment (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_card_comment_card FOREIGN KEY (card_id) REFERENCES card(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_comment_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_card_comment_card_id ON card_comment(card_id);
CREATE INDEX idx_card_comment_created_at ON card_comment(created_at);
