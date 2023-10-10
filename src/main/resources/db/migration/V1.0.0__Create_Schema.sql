    create sequence Extension_SEQ;

    create sequence ExtensionInstall_SEQ;

    create table Extension (
        id bigint default nextval('Extension_SEQ') not null,
        active boolean not null,
        displayName varchar(255),
        icon varchar(500),
        name varchar(255) unique,
        primary key (id)
    );

    create table ExtensionInstall (
        id bigint default nextval('ExtensionInstall_SEQ') not null,
        delta integer not null,
        installs integer not null,
        onpremDownloads integer not null,
        time timestamp(6) with time zone,
        total_installs integer not null,
        updates integer not null,
        version varchar(255),
        extension_id bigint,
        primary key (id)
    );

    alter table if exists ExtensionInstall
       add constraint FKjbd2cuyi4va86y6tqg10623ek
       foreign key (extension_id)
       references Extension
       on delete cascade;