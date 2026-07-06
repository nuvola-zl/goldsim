create table if not exists goldsim.mall_activity
(
    id            bigint auto_increment
    primary key,
    activity_name varchar(100)                       not null comment '活动名称，如"618黄金抢购"',
    start_time    datetime                           not null comment '活动开始时间',
    end_time      datetime                           not null comment '活动结束时间',
    status        tinyint  default 0                 null comment '0-待开始 1-进行中 2-已结束 3-已取消',
    create_time   datetime default CURRENT_TIMESTAMP null,
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
    )
    comment '营销活动表（只支持抢购）';

create index idx_status_endtime
    on goldsim.mall_activity (status, end_time);

create index idx_status_time
    on goldsim.mall_activity (status, start_time);




create table if not exists goldsim.mall_activity_goods
(
    id             bigint auto_increment
    primary key,
    activity_id    bigint                             not null comment '关联活动',
    spec_id        bigint                             not null comment '关联商品规格ID',
    original_price decimal(18, 2)                     not null comment '原价（活动前的总价格）',
    activity_price decimal(18, 2)                     not null comment '活动价（抢购时的总价格）',
    activity_stock int                                not null comment '活动库存',
    limit_per_user int      default 1                 null comment '每人限购数量',
    sold_count     int      default 0                 null comment '已售数量',
    create_time    datetime default CURRENT_TIMESTAMP null,
    constraint uk_activity_spec
    unique (activity_id, spec_id)
    )
    comment '活动商品关联表';

create index idx_activity_id
    on goldsim.mall_activity_goods (activity_id);

create index idx_spec_activity
    on goldsim.mall_activity_goods (spec_id, activity_id);


create table if not exists goldsim.mall_goods
(
    id          bigint auto_increment comment '商品主ID'
    primary key,
    goods_name  varchar(100)                       not null comment '商品名称',
    category    varchar(20)                        not null comment 'CATEGORY_GOLD_BAR-投资金条 CATEGORY_JEWELRY-首饰 CATEGORY_COIN-金币',
    metal_type  varchar(10)                        not null comment 'AU-黄金 AG-白银 PT-铂金',
    store_id    bigint                             not null comment '所属店铺ID（预留，店铺表后续建）',
    sort        int      default 0                 null comment '排序',
    create_time datetime default CURRENT_TIMESTAMP null,
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
    )
    comment '商城商品主表';

create index idx_category_metal_sort
    on goldsim.mall_goods (category, metal_type, sort);


create table if not exists goldsim.mall_goods_spec
(
    id             bigint auto_increment
    primary key,
    goods_id       bigint                                   not null comment '关联商品主表ID',
    weight         decimal(10, 2)                           not null comment '克重：10.00、20.00',
    craft_fee      decimal(18, 2) default 0.00              null comment '工费（元/克），可选，默认0',
    price_per_gram decimal(18, 2)                           not null comment '实时金价（元/克）',
    total_price    decimal(18, 2)                           not null comment '总价格 = 实时金价×克重 + 工费×克重',
    stock          int            default 0                 null comment '当前库存',
    sale_status    tinyint        default 1                 null comment '0-停售 1-在售',
    cover_image    varchar(500)                             null comment '规格封面图URL',
    detail_images  json                                     null comment '规格详情图数组',
    description    text                                     null comment '规格描述（富文本）',
    create_time    datetime       default CURRENT_TIMESTAMP null,
    update_time    datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    frozen_stock   int            default 0                 not null comment '下单未支付冻结库存',
    constraint uk_goods_weight
    unique (goods_id, weight)
    )
    comment '商品规格表';

create index idx_goods_id
    on goldsim.mall_goods_spec (goods_id);

create index idx_sale_goods_weight
    on goldsim.mall_goods_spec (sale_status, goods_id, weight);



create table if not exists goldsim.store
(
    id             bigint auto_increment comment '门店ID'
    primary key,
    store_name     varchar(100)                            not null comment '门店名称，如"北京国贸旗舰店"',
    province       varchar(50)                             null comment '省',
    city           varchar(50)                             null comment '市',
    district       varchar(50)                             null comment '区/县',
    address        varchar(500)                            not null comment '详细地址',
    longitude      decimal(10, 7)                          not null comment '经度',
    latitude       decimal(10, 7)                          not null comment '纬度',
    phone          varchar(20)                             null comment '门店电话',
    business_hours varchar(50)                             null comment '营业时间，如"09:00-21:00"',
    cover_image    varchar(500)                            null comment '门店封面图，列表展示用',
    rating         decimal(2, 1) default 5.0               null comment '门店评分 0.0-5.0',
    rating_count   int           default 0                 null comment '评价总人数',
    status         tinyint       default 1                 null comment '0-停业 1-营业中',
    create_time    datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
    )
    comment '门店信息表' charset = utf8mb4;

create index idx_city
    on goldsim.store (city);

create index idx_lng_lat
    on goldsim.store (longitude, latitude);

create index idx_status
    on goldsim.store (status);

