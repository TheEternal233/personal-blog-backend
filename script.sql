create table qrtz_calendars
(
    SCHED_NAME    varchar(120) not null,
    CALENDAR_NAME varchar(200) not null,
    CALENDAR      blob         not null,
    primary key (SCHED_NAME, CALENDAR_NAME)
)
    row_format = DYNAMIC;

create table qrtz_fired_triggers
(
    SCHED_NAME        varchar(120) not null,
    ENTRY_ID          varchar(95)  not null,
    TRIGGER_NAME      varchar(200) not null,
    TRIGGER_GROUP     varchar(200) not null,
    INSTANCE_NAME     varchar(200) not null,
    FIRED_TIME        bigint       not null,
    SCHED_TIME        bigint       not null,
    PRIORITY          int          not null,
    STATE             varchar(16)  not null,
    JOB_NAME          varchar(200) null,
    JOB_GROUP         varchar(200) null,
    IS_NONCONCURRENT  varchar(1)   null,
    REQUESTS_RECOVERY varchar(1)   null,
    primary key (SCHED_NAME, ENTRY_ID)
)
    row_format = DYNAMIC;

create table qrtz_job_details
(
    SCHED_NAME        varchar(120) not null,
    JOB_NAME          varchar(200) not null,
    JOB_GROUP         varchar(200) not null,
    DESCRIPTION       varchar(250) null,
    JOB_CLASS_NAME    varchar(250) not null,
    IS_DURABLE        varchar(1)   not null,
    IS_NONCONCURRENT  varchar(1)   not null,
    IS_UPDATE_DATA    varchar(1)   not null,
    REQUESTS_RECOVERY varchar(1)   not null,
    JOB_DATA          blob         null,
    primary key (SCHED_NAME, JOB_NAME, JOB_GROUP)
)
    row_format = DYNAMIC;

create table qrtz_locks
(
    SCHED_NAME varchar(120) not null,
    LOCK_NAME  varchar(40)  not null,
    primary key (SCHED_NAME, LOCK_NAME)
)
    row_format = DYNAMIC;

create table qrtz_paused_trigger_grps
(
    SCHED_NAME    varchar(120) not null,
    TRIGGER_GROUP varchar(200) not null,
    primary key (SCHED_NAME, TRIGGER_GROUP)
)
    row_format = DYNAMIC;

create table qrtz_scheduler_state
(
    SCHED_NAME        varchar(120) not null,
    INSTANCE_NAME     varchar(200) not null,
    LAST_CHECKIN_TIME bigint       not null,
    CHECKIN_INTERVAL  bigint       not null,
    primary key (SCHED_NAME, INSTANCE_NAME)
)
    row_format = DYNAMIC;

create table qrtz_triggers
(
    SCHED_NAME     varchar(120) not null,
    TRIGGER_NAME   varchar(200) not null,
    TRIGGER_GROUP  varchar(200) not null,
    JOB_NAME       varchar(200) not null,
    JOB_GROUP      varchar(200) not null,
    DESCRIPTION    varchar(250) null,
    NEXT_FIRE_TIME bigint       null,
    PREV_FIRE_TIME bigint       null,
    PRIORITY       int          null,
    TRIGGER_STATE  varchar(16)  not null,
    TRIGGER_TYPE   varchar(8)   not null,
    START_TIME     bigint       not null,
    END_TIME       bigint       null,
    CALENDAR_NAME  varchar(200) null,
    MISFIRE_INSTR  smallint     null,
    JOB_DATA       blob         null,
    primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    constraint qrtz_triggers_ibfk_1
        foreign key (SCHED_NAME, JOB_NAME, JOB_GROUP) references qrtz_job_details (SCHED_NAME, JOB_NAME, JOB_GROUP)
)
    row_format = DYNAMIC;

create table qrtz_blob_triggers
(
    SCHED_NAME    varchar(120) not null,
    TRIGGER_NAME  varchar(200) not null,
    TRIGGER_GROUP varchar(200) not null,
    BLOB_DATA     blob         null,
    primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    constraint qrtz_blob_triggers_ibfk_1
        foreign key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) references qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
)
    row_format = DYNAMIC;

create table qrtz_cron_triggers
(
    SCHED_NAME      varchar(120) not null,
    TRIGGER_NAME    varchar(200) not null,
    TRIGGER_GROUP   varchar(200) not null,
    CRON_EXPRESSION varchar(200) not null,
    TIME_ZONE_ID    varchar(80)  null,
    primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    constraint qrtz_cron_triggers_ibfk_1
        foreign key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) references qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
)
    row_format = DYNAMIC;

create table qrtz_simple_triggers
(
    SCHED_NAME      varchar(120) not null,
    TRIGGER_NAME    varchar(200) not null,
    TRIGGER_GROUP   varchar(200) not null,
    REPEAT_COUNT    bigint       not null,
    REPEAT_INTERVAL bigint       not null,
    TIMES_TRIGGERED bigint       not null,
    primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    constraint qrtz_simple_triggers_ibfk_1
        foreign key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) references qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
)
    row_format = DYNAMIC;

create table qrtz_simprop_triggers
(
    SCHED_NAME    varchar(120)   not null,
    TRIGGER_NAME  varchar(200)   not null,
    TRIGGER_GROUP varchar(200)   not null,
    STR_PROP_1    varchar(512)   null,
    STR_PROP_2    varchar(512)   null,
    STR_PROP_3    varchar(512)   null,
    INT_PROP_1    int            null,
    INT_PROP_2    int            null,
    LONG_PROP_1   bigint         null,
    LONG_PROP_2   bigint         null,
    DEC_PROP_1    decimal(13, 4) null,
    DEC_PROP_2    decimal(13, 4) null,
    BOOL_PROP_1   varchar(1)     null,
    BOOL_PROP_2   varchar(1)     null,
    primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    constraint qrtz_simprop_triggers_ibfk_1
        foreign key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) references qrtz_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
)
    row_format = DYNAMIC;

create index SCHED_NAME
    on qrtz_triggers (SCHED_NAME, JOB_NAME, JOB_GROUP);

create table sys_log
(
    id               bigint auto_increment comment '编号'
        primary key,
    module           varchar(50)             not null comment '模块名称',
    operation        varchar(50)             not null comment '操作',
    user_name        varchar(50)             not null comment '操作人员',
    ip               varchar(100)            not null comment 'ip地址',
    address          varchar(255) default '' null comment '地址',
    state            tinyint(1)              not null comment '操作状态(0：成功，1：失败，2：异常)',
    method           varchar(255)            not null comment '操作方法',
    req_parameter    longtext                null comment '请求参数',
    req_mapping      varchar(10)             not null comment '请求方式',
    exception        longtext                null comment '异常信息',
    return_parameter longtext                null comment '返回参数',
    req_address      varchar(255)            not null comment '请求地址',
    time             bigint                  not null comment '消耗时间(ms)',
    description      varchar(100)            null comment '接口描述',
    create_time      datetime                not null comment '创建时间',
    update_time      datetime                not null comment '更新时间',
    is_deleted       tinyint(1)   default 0  not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table sys_login_log
(
    id          bigint auto_increment comment '日志编号'
        primary key,
    user_name   varchar(50)                    not null comment '用户名称',
    ip          varchar(50)                    not null comment '登录ip',
    address     varchar(50) default '未知地址' not null comment '登录地址',
    browser     varchar(50)                    not null comment '浏览器',
    os          varchar(50)                    not null comment '操作系统',
    type        tinyint(1)                     not null comment '登录类型(0：前台，1：后台，2：非法登录)',
    state       tinyint(1)                     not null comment '登录状态(0：成功，1：失败)',
    message     longtext                       not null comment '登录信息',
    create_time datetime                       not null comment '用户创建时间',
    update_time datetime                       not null comment '用户更新时间',
    is_deleted  tinyint(1)  default 0          not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table sys_menu
(
    id                    bigint auto_increment comment '唯一id'
        primary key,
    title                 varchar(50)          not null comment '标题',
    icon                  varchar(50)          null comment '图标',
    path                  varchar(255)         not null comment '地址',
    component             varchar(255)         null comment '绑定的哪个组件，默认自带的组件类型分别是：Iframe、RouteView和ComponentError',
    redirect              varchar(255)         null comment '父菜单重定向地址(默认第一个子菜单)',
    affix                 tinyint    default 0 not null comment '是否是固定页签(0否 1是)',
    parent_id             bigint               null comment '父级菜单的id',
    name                  varchar(255)         null comment '同路由中的name，主要是用于保活的左右',
    hide_in_menu          tinyint    default 0 not null comment '是否隐藏当前菜单(0否 1是)',
    url                   varchar(255)         null comment '如果当前是iframe的模式，需要有一个跳转的url支撑，其不能和path重复，path还是为路由',
    hide_in_breadcrumb    tinyint    default 1 not null comment '是否存在于面包屑(0否 1是)',
    hide_children_in_menu tinyint    default 1 not null comment '是否不需要显示所有的子菜单(0否 1是)',
    keep_alive            tinyint    default 1 not null comment '是否保活(0否 1是)',
    target                varchar(255)         null comment '全连接跳转模式(''_blank'' | ''_self'' | ''_parent'')',
    is_disable            tinyint(1) default 0 not null comment '是否禁用 (0否 1是)',
    order_num             int        default 1 not null comment '排序',
    create_time           datetime             not null comment '创建时间',
    update_time           datetime             not null comment '更新时间',
    is_deleted            tinyint    default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table sys_permission
(
    id              bigint unsigned auto_increment comment '权限表id'
        primary key,
    permission_desc varchar(50)       not null comment '描述',
    permission_key  varchar(255)      not null comment '权限字符',
    menu_id         bigint            not null comment '菜单id',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          not null comment '更新时间',
    is_deleted      tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table sys_role
(
    id          bigint unsigned auto_increment comment '角色id'
        primary key,
    role_name   varchar(100)      not null comment '角色名称',
    role_key    varchar(10)       not null comment '角色字符',
    status      tinyint default 0 not null comment '状态（0：正常，1：停用）',
    order_num   bigint            not null comment '排序',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          not null comment '更新时间',
    is_deleted  tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table sys_role_menu
(
    id         int auto_increment comment '主键'
        primary key,
    role_id    bigint            not null comment '角色id',
    menu_id    bigint            not null comment '菜单id',
    is_deleted tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table sys_role_permission
(
    id            bigint auto_increment comment '关系表id'
        primary key,
    role_id       bigint not null comment '角色id',
    permission_id bigint not null comment '权限id'
)
    row_format = DYNAMIC;

create table sys_user
(
    id               bigint unsigned auto_increment comment '用户id'
        primary key,
    nickname         varchar(50)             null comment '用户昵称',
    username         varchar(50)             not null comment '用户名',
    gender           tinyint(1)   default 0  not null comment '用户性别(0,未定义,1,男,2女)',
    password         varchar(100)            null comment '用户密码',
    avatar           varchar(255)            not null comment '用户头像',
    intro            varchar(100)            null comment '个人简介',
    email            varchar(50)             null comment '用户邮箱',
    register_ip      varchar(100)            not null comment '注册ip',
    register_type    tinyint                 not null comment '注册方式(0邮箱/姓名 1Gitee 2Github)',
    register_address varchar(255) default '' null comment '注册地址',
    login_ip         varchar(100)            null comment '最近登录ip',
    login_address    varchar(50)             null comment '最近登录地址',
    login_type       tinyint                 null comment '最近登录类型(0邮箱/姓名 1Gitee 2Github)',
    login_time       datetime                not null comment '用户最近登录时间',
    is_disable       tinyint(1)   default 0  not null comment '是否禁用 (0否 1是)',
    create_time      datetime                not null comment '用户创建时间',
    update_time      datetime                not null comment '用户更新时间',
    is_deleted       tinyint(1)   default 0  not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table ai_session
(
    id         bigint unsigned auto_increment comment '会话ID'
        primary key,
    user_id    bigint unsigned                    not null comment '用户ID',
    title      varchar(255)                       not null comment '会话标题',
    preview    varchar(500)                       not null comment '会话预览（通常是第一条或最新消息摘要）',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    model      varchar(100)                       null,
    constraint fk_ai_session_user
        foreign key (user_id) references sys_user (id)
            on update cascade on delete cascade
)
    comment 'AI聊天会话表' collate = utf8mb4_unicode_ci;

create table ai_message
(
    id         bigint unsigned auto_increment comment '消息ID'
        primary key,
    session_id bigint unsigned                    not null comment '会话ID',
    role       enum ('user', 'assistant')         not null comment '消息角色（user：用户，assistant：AI助手）',
    content    text                               not null comment '消息内容',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint fk_ai_message_session
        foreign key (session_id) references ai_session (id)
            on update cascade on delete cascade
)
    comment 'AI聊天消息表' collate = utf8mb4_unicode_ci;

create index idx_session_created
    on ai_message (session_id, created_at);

create index idx_session_id
    on ai_message (session_id);

create index idx_user_id
    on ai_session (user_id);

create table sys_user_role
(
    id      int auto_increment comment '主键'
        primary key,
    user_id int         not null comment '用户id',
    role_id varchar(20) not null comment '角色id'
)
    row_format = DYNAMIC;

create table sys_website_info
(
    id                           bigint auto_increment
        primary key,
    webmaster_avatar             varchar(100)         null comment '站长头像',
    webmaster_name               varchar(50)          null comment '站长名称',
    webmaster_copy               varchar(100)         null comment '站长文案',
    webmaster_profile_background varchar(255)         null comment '站长资料卡背景图',
    gitee_link                   varchar(100)         null comment 'gitee链接',
    github_link                  varchar(100)         null comment 'github链接',
    website_name                 varchar(50)          null comment '网站名称',
    header_notification          varchar(150)         null comment '头部通知',
    sidebar_announcement         varchar(255)         null comment '侧面公告',
    record_info                  varchar(255)         null comment '备案信息',
    start_time                   datetime             null comment '开始运行时间',
    create_time                  datetime             not null comment '用户创建时间',
    update_time                  datetime             not null comment '用户更新时间',
    is_deleted                   tinyint(1) default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_article
(
    id              bigint unsigned auto_increment comment '文章id'
        primary key,
    user_id         bigint            not null comment '作者id',
    category_id     bigint            not null comment '分类id',
    article_cover   varchar(1024)     not null comment '文章缩略图',
    article_title   varchar(50)       not null comment '文章标题',
    article_content longtext          not null comment '文章内容',
    article_type    tinyint           not null comment '类型 (1原创 2转载 3翻译)',
    is_top          tinyint           not null comment '是否置顶 (0否 1是）',
    status          tinyint           not null comment '文章状态 (1公开 2私密 3草稿)',
    visit_count     bigint  default 0 not null comment '访问量',
    create_time     datetime          not null comment '文章创建时间',
    update_time     datetime          not null comment '文章更新时间',
    is_deleted      tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_article_tag
(
    id          bigint unsigned auto_increment comment '关系表id'
        primary key,
    article_id  bigint unsigned   not null comment '文章id',
    tag_id      bigint            not null comment '标签id',
    create_time datetime          not null comment '创建时间',
    is_deleted  tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_banners
(
    id          bigint auto_increment comment '主键id'
        primary key,
    path        varchar(255) not null comment '图片路径',
    size        bigint       not null comment '图片大小 (字节)',
    type        varchar(50)  not null comment '图片类型 (MIME)',
    user_id     bigint       not null comment '上传人id',
    sort_order  int          not null comment '图片顺序',
    create_time datetime     not null comment '创建时间'
)
    collate = utf8mb4_unicode_ci
    row_format = DYNAMIC;

create table t_black_list
(
    id           bigint auto_increment comment '主键id'
        primary key,
    user_id      bigint                             null comment '用户id',
    reason       varchar(255)                       null comment '拉黑原因',
    banned_time  datetime                           null comment '封禁时间',
    expires_time datetime                           null comment '过期时间',
    type         tinyint                            null comment '类型：1-用户 2-IP',
    ip_info      varchar(255)                       null comment 'IP信息',
    create_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_deleted   tinyint  default 0                 null comment '是否删除：0-未删除 1-已删除'
)
    comment '黑名单表' collate = utf8mb4_unicode_ci;

create table t_category
(
    id            bigint unsigned auto_increment comment '分类id'
        primary key,
    category_name varchar(20)       not null comment '分类名',
    create_time   datetime          not null comment '创建时间',
    update_time   datetime          not null comment '更新时间',
    is_deleted    tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_comment
(
    id              bigint auto_increment comment '评论id'
        primary key,
    type            tinyint(1)           not null comment '评论类型 (1文章 2留言板)',
    type_id         bigint               not null comment '类型id',
    parent_id       bigint               null comment '父评论id',
    reply_id        bigint               null comment '回复评论id',
    comment_content text                 not null comment '评论的内容',
    comment_user_id bigint               not null comment '评论用户的id',
    reply_user_id   bigint               null comment '回复用户的id',
    is_check        tinyint(1) default 1 not null comment '是否通过 (0否 1是)',
    create_time     datetime             not null comment '评论时间',
    update_time     datetime             not null comment '更新时间',
    is_deleted      tinyint(1) default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_favorite
(
    id          bigint auto_increment comment '收藏id'
        primary key,
    user_id     bigint               not null comment '收藏的用户id',
    type        tinyint              not null comment '收藏类型(1,文章 2,留言板)',
    type_id     bigint               not null comment '类型id',
    is_check    tinyint(1) default 1 not null comment '是否有效 (0否 1是)',
    create_time datetime             not null comment '收藏时间',
    is_deleted  tinyint(1) default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_leave_word
(
    id          bigint auto_increment comment 'id'
        primary key,
    user_id     bigint               not null comment '留言用户id',
    content     longtext             not null comment '留言内容',
    is_check    tinyint(1) default 1 not null comment '是否通过 (0否 1是)',
    create_time datetime             not null comment '留言时间',
    update_time datetime             not null comment '更新时间',
    is_deleted  tinyint(1) default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_like
(
    id          bigint auto_increment comment '点赞表id'
        primary key,
    user_id     bigint   not null comment '点赞的用户id',
    type        tinyint  not null comment '点赞类型(1,文章,2,评论,3留言板)',
    type_id     bigint   not null comment '点赞的文章id',
    create_time datetime not null comment '点赞时间',
    update_time datetime not null comment '修改时间'
)
    row_format = DYNAMIC;

create table t_link
(
    id          bigint auto_increment comment '友链表id'
        primary key,
    user_id     bigint            not null comment '用户id',
    name        varchar(100)      not null comment '网站名称',
    url         varchar(100)      not null comment '网站地址',
    description varchar(100)      not null comment '网站描述',
    background  varchar(100)      not null comment '网站背景',
    is_check    tinyint default 0 not null comment '审核状态（0：未通过，1：已通过）',
    email       varchar(20)       not null comment '邮箱地址',
    create_time datetime          not null comment '创建时间',
    update_time datetime          not null comment '更新时间',
    is_deleted  tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_photo
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    name        varchar(255)                         null comment '照片名称',
    type        int        default 0                 null comment '图片类型(0:图片,1:文件夹)',
    url         varchar(500)                         null comment '照片地址',
    parent_id   bigint                               null comment '父级相册ID',
    sort        int                                  null comment '排序',
    is_deleted  tinyint    default 0                 null comment '是否删除(0-未删,1-已删)',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    user_id     bigint                               null comment '用户ID',
    size        decimal(10, 2)                       null comment '文件大小(MB)',
    description varchar(500)                         null comment '描述',
    is_check    tinyint(1) default 0                 null comment '是否审核'
)
    comment '相册照片表';

create table t_tag
(
    id          bigint unsigned auto_increment comment '标签id'
        primary key,
    tag_name    varchar(20)       not null comment '标签名称',
    create_time datetime          not null comment '标签创建时间',
    update_time datetime          not null comment '标签更新时间',
    is_deleted  tinyint default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;

create table t_tree_hole
(
    id          bigint auto_increment comment '树洞表id'
        primary key,
    user_id     bigint               not null comment '用户id',
    content     varchar(100)         not null comment '内容',
    is_check    tinyint(1) default 1 not null comment '是否通过 (0否 1是)',
    create_time datetime             not null comment '创建时间',
    update_time datetime             not null comment '修改时间',
    is_deleted  tinyint(1) default 0 not null comment '是否删除（0：未删除，1：已删除）'
)
    row_format = DYNAMIC;


