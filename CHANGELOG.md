# Changelog
All notable changes to this project will be documented in this file.

## [9.0.0] - 2019-03-29
### Added
- Added DashboardProcessInstanceDetails model to get everything related to the process instance such as request document, last activity instance etc.
- Sending emails for pending actions and transitions in the business process

### Changed
- Removed binary contents from the logs

## [8.0.0] - 2019-02-28
### Added
- Multilinguality-related updates

## [7.0.0] - 2019-02-04
### Added
- Added resource ownership mechanism

### Changed
- Improved Swagger documentation and provided a detailed external documentation
- All REST endpoints are expecting a token
- Disabled abandoning db connections
- Increase db connection size
- Authorization header is added to all services to validate the user performing the operation
- Hjid checks for validating resources such as business process documents

## [6.0.0] - 2018-12-21
### Added
- Introduced Collaboration Group concept keeping bilateral communication histories as individual entries
    e.g. from a seller point of view, activities with an initial buyer and a transport 
    service provider are kept in separated communication histories

### Changed
- Binary contents exchanged in business processes are managed in the new binary content database
- Switched to Spring-managed data repositories
- Code generation is now performed manually

## [5.0.0] - 2018-11-02
### Added
- Cancellation option for individual business processes as well as whole collaboration
    Email notification in case of cancellations
    Seller and buyer’s feedback on cancellations that is further used for platform participants management and possible negotiation resolutions.
- Collaboration histories keeping the ratings and comments
  Rating and comments about different aspects of collaboration (communication, delivery and packaging, fulfillment of terms, response time, etc)
  ,which are further used by the trust management to evaluate reputation of platform participants

### Changed
- Keeping a reference to the users initiating a transaction inside business processes
- Multiple files and notes can be exchanged in any business process

## [4.0.0] - 2018-09-14
### Added
- Capability to update and cancel business processes

### Changed
- Backend service to get order content in whole business process group
- Added search filter for business processes based on the process status

## [3.0.0] - 2018-06-01
### Added
- Execute (Business-Process) according-to (Contract) (new)
- Monitor (Business-Process) using (Platform data channels) (new)

 ---
The project leading to this application has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 723810.
