CREATE DATABASE lectures;

CREATE TABLE lecture
(
    id character varying PRIMARY KEY NOT NULL,
    index integer NOT NULL,
    name character varying NOT NULL,
    course character varying NOT NULL,
    professor character varying NOT NULL,
    "time" character varying
);

CREATE TABLE alarm
(
	id SERIAL PRIMARY KEY,
    user_id character varying NOT NULL,
    lecture_id character varying NOT NULL,
    CONSTRAINT "lecture.id" FOREIGN KEY (lecture_id)
        REFERENCES lecture (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
