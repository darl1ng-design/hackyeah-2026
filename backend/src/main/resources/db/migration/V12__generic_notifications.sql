alter table user_notification alter column idea_id drop not null;
alter table user_notification alter column kind type varchar(32);
alter table user_notification add column target_type varchar(32);
alter table user_notification add column target_id bigint;

update user_notification
set target_type = 'IDEA', target_id = idea_id;

alter table user_notification alter column target_type set not null;
alter table user_notification alter column target_id set not null;

alter table user_notification drop constraint if exists user_notification_kind_check;
alter table user_notification drop constraint if exists chk_user_notification_kind;
alter table user_notification add constraint chk_user_notification_kind
    check (kind in ('NEW_IDEA', 'IDEA_REPLY', 'NEW_GRANT_APPLICATION', 'MENTOR_MESSAGE', 'GRANT_STATUS', 'TESTER_ACTIVITY'));
alter table user_notification add constraint chk_user_notification_target_type
    check (target_type in ('IDEA', 'MENTOR_CONVERSATION', 'GRANT_APPLICATION', 'TESTER_FEEDBACK'));

create index idx_user_notification_target on user_notification (target_type, target_id);
